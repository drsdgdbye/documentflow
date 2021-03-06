package com.documentflow;

import com.documentflow.entities.Address;
import com.documentflow.entities.Contragent;
import com.documentflow.entities.Organization;
import com.documentflow.entities.Person;
import com.documentflow.entities.dto.ContragentDto;
import com.documentflow.entities.dto.ContragentDtoAddress;
import com.documentflow.entities.dto.ContragentDtoEmployee;
import com.documentflow.entities.dto.ContragentDtoParameters;
import com.documentflow.repositories.AddressRepository;
import com.documentflow.repositories.ContragentRepository;
import com.documentflow.repositories.OrganizationRepository;
import com.documentflow.repositories.PersonRepository;
import com.documentflow.utils.ContragentUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DocumentflowApplication.class})
@Rollback
@Transactional
@ActiveProfiles("test")
public abstract class AbstractDocumentFlowTest {

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private ContragentRepository contragentRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    protected static final String PERSON = "person";
    protected static final String COMPANY = "company";

    protected Address createRandomAddress() {
        return Address.builder()
                .index(RandomUtils.nextInt(100000, 700000))
                .country(RandomStringUtils.randomAlphabetic(7))
                .city(RandomStringUtils.randomAlphabetic(7))
                .street(RandomStringUtils.randomAlphabetic(7))
                .houseNumber(RandomStringUtils.randomNumeric(2))
                .apartmentNumber(RandomStringUtils.randomNumeric(3))
                .build();
    }

    protected Person createRandomPerson() {
        return new Person(null,
                RandomStringUtils.randomAlphabetic(7),
                RandomStringUtils.randomAlphabetic(7),
                RandomStringUtils.randomAlphabetic(7));
    }

    protected Contragent createRandomPersonContragent(Address address, Person person) {
        String searchString = ContragentUtils.toUpperCase(person.getFirstName()) +
                ContragentUtils.toUpperCase(person.getMiddleName()) +
                ContragentUtils.toUpperCase(person.getLastName());

        Contragent contragent = new Contragent.Builder()
                .address(address)
                .person(person)
                .isDeleted(false)
                .searchName(searchString)
                .build();
        return contragentRepository.save(contragent);
    }

    protected Contragent createRandomOrganizationContragent(Address address, Organization organization, Person person) {
        String searchString = organization.getName();

        Contragent contragent = new Contragent.Builder()
                .address(address)
                .organization(organization)
                .person(person)
                .isDeleted(false)
                .searchName(searchString)
                .personPosition(RandomStringUtils.randomAlphabetic(7))
                .build();
        return contragentRepository.save(contragent);
    }

    protected ContragentDtoAddress createRandomDtoAddress() {
        return new ContragentDtoAddress(RandomStringUtils.randomNumeric(7, 8),
                RandomStringUtils.randomNumeric(7, 8),
                RandomStringUtils.randomAlphabetic(7),
                RandomStringUtils.randomAlphabetic(7),
                RandomStringUtils.randomAlphabetic(7),
                RandomStringUtils.randomNumeric(2),
                RandomStringUtils.randomNumeric(3));
    }

    protected ContragentDtoEmployee createRandomDtoEmployee() {
        return new ContragentDtoEmployee(RandomStringUtils.randomNumeric(7, 8),
                RandomStringUtils.randomAlphabetic(7),
                RandomStringUtils.randomAlphabetic(7),
                RandomStringUtils.randomAlphabetic(7),
                RandomStringUtils.randomAlphabetic(7));
    }

    protected ContragentDtoParameters createRandomDtoParametersPerson() {
        return new ContragentDtoParameters(RandomStringUtils.randomAlphabetic(7),
                RandomStringUtils.randomAlphabetic(7),
                RandomStringUtils.randomAlphabetic(7),
                null);
    }

    protected ContragentDtoParameters createRandomDtoParametersCompany() {
        return new ContragentDtoParameters(null,
                null,
                null,
                RandomStringUtils.randomAlphabetic(7));
    }

    protected ContragentDto createRandomDtoOrganization() {
        return new ContragentDto(COMPANY,
                createRandomDtoParametersCompany(),
                new ContragentDtoAddress[]{createRandomDtoAddress()},
                new ContragentDtoEmployee[]{createRandomDtoEmployee()});
    }

    protected ContragentDto createRandomDtoPerson() {
        return new ContragentDto(PERSON,
                createRandomDtoParametersPerson(),
                new ContragentDtoAddress[]{createRandomDtoAddress()},
                null);
    }

    protected Address createAndSaveRandomAddress() {
        return createAndSaveRandomEntity(Address.class);
    }

    protected Person createAndSaveRandomPerson() {
        return createAndSaveRandomEntity(Person.class);
    }

    protected Contragent createAndSaveRandomContragent() {
        return createAndSaveRandomEntity(Contragent.class);
    }

    protected Organization createRandomOrganization() {
        return new Organization(null, RandomStringUtils.randomAlphabetic(5));
    }

    protected Organization createAndSaveRandomOrganization() {
        Organization organization = new Organization(null, RandomStringUtils.randomAlphabetic(5));
        organization = organizationRepository.save(organization);

        Address address = normalizeAddress(createRandomAddress());
        address = addressRepository.save(address);

        Person person = normalizePerson(createRandomPerson());
        person = personRepository.save(person);

        Contragent contragent = createRandomOrganizationContragent(address, organization, person);
        address.setContragents(Collections.singletonList(contragent));
        address.setOrganizations(Collections.singletonList(organization));
        address.setPersons(Collections.singletonList(person));
        organization.setContragents(Collections.singletonList(contragent));
        organization.setAddresses(Collections.singletonList(address));
        organization.setPersons(Collections.singletonList(person));
        return organization;
    }

    private Address normalizeAddress(Address address) {
        address.setCountry(address.getCountry().toUpperCase());
        address.setCity(address.getCity().toUpperCase());
        address.setStreet(address.getStreet().toUpperCase());
        return address;
    }

    private Person normalizePerson(Person person) {
        person.setFirstName(person.getFirstName().toUpperCase());
        person.setMiddleName(person.getMiddleName().toUpperCase());
        person.setLastName(person.getLastName().toUpperCase());
        return person;
    }

    private <T> T createAndSaveRandomEntity(Class<T> tClass) {
        Address address = normalizeAddress(createRandomAddress());
        address = addressRepository.save(address);
        Person person = personRepository.save(createRandomPerson());
        Contragent contragent = createRandomPersonContragent(address, person);
        address.setContragents(Collections.singletonList(contragent));
        person.setContragents(Collections.singletonList(contragent));
        if (tClass.isInstance(address)) {
            return (T) address;
        }
        if (tClass.isInstance(person)) {
            return (T) person;
        }
        if (tClass.isInstance(contragent)) {
            return (T) contragent;
        }
        return null;
    }
}
