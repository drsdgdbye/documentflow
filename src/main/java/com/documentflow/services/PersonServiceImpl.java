package com.documentflow.services;

import com.documentflow.entities.Address;
import com.documentflow.entities.Contragent;
import com.documentflow.entities.Person;
import com.documentflow.entities.dto.ContragentDtoEmployee;
import com.documentflow.entities.dto.ContragentDtoParameters;
import com.documentflow.exceptions.NotFoundIdException;
import com.documentflow.exceptions.NotFoundPersonException;
import com.documentflow.repositories.PersonRepository;
import com.documentflow.repositories.specifications.PersonSpecifications;
import com.documentflow.utils.ContragentUtils;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class PersonServiceImpl implements PersonService {

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private ContragentService contragentService;

    @Override
    public Person save(@NonNull ContragentDtoParameters contragentDto) {

        String firstName = ContragentUtils.toUpperCase(contragentDto.getFirstName());
        String middleName = ContragentUtils.toUpperCase(contragentDto.getMiddleName());
        String lastName = ContragentUtils.toUpperCase(contragentDto.getLastName());

        Person personWithoutId = new Person(firstName, middleName, lastName);
        return personRepository.save(personWithoutId);
    }

    @Override
    public Person save(Person person) {
        return personRepository.save(person);
    }

    @Override
    public Map<Person, String> save(ContragentDtoEmployee[] employees) {

        return Arrays.stream(employees)
                .filter(ContragentUtils::isNotEmpty)
                .collect(Collectors.toMap(
                        (ContragentDtoEmployee key) -> {
                            Person person = new Person(key.getFirstName(), key.getMiddleName(), key.getLastName());
                            return personRepository.save(person);
                        },
                        ContragentDtoEmployee::getPersonPosition
                ));
    }

    @Override
    public Person find(Long id) {
        Optional<Person> optionalPerson = personRepository.findById(id);
        if (!optionalPerson.isPresent()) {
            throw new NotFoundPersonException();
        }
        return optionalPerson.get();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Person> findAll(String firstName, String middleName, String lastName) {
        Specification<Person> spec = Specification.where(null);
        if (StringUtils.isNotEmpty(firstName)) {
            spec = spec.and(PersonSpecifications.firstNameEq(firstName.toUpperCase()));
        }
        if (StringUtils.isNotEmpty(middleName)) {
            spec = spec.and(PersonSpecifications.middleNameEq(middleName.toUpperCase()));
        }
        if (StringUtils.isNotEmpty(lastName)) {
            spec = spec.and(PersonSpecifications.lastNameEq(lastName.toUpperCase()));
        }

        return personRepository.findAll(spec).stream()
                .filter(person -> {
                    return person.getContragents().stream()
                            .anyMatch(c -> c.getOrganization() == null && !c.getIsDeleted());
                })
                .collect(Collectors.toList());
    }

    @Override
    public Person strongFind(Person person) {
        Specification<Person> spec = Specification.where(null);
        if (StringUtils.isNotEmpty(person.getFirstName())) {
            spec = spec.and(PersonSpecifications.firstNameEq(person.getFirstName().toUpperCase()));
        } else {
            spec = spec.and(PersonSpecifications.firstNameIsNull());
        }
        if (StringUtils.isNotEmpty(person.getMiddleName())) {
            spec = spec.and(PersonSpecifications.middleNameEq(person.getMiddleName().toUpperCase()));
        } else {
            spec = spec.and(PersonSpecifications.middleNameIsNull());
        }
        if (StringUtils.isNotEmpty(person.getLastName())) {
            spec = spec.and(PersonSpecifications.lastNameEq(person.getLastName().toUpperCase()));
        }
        return personRepository.findOne(spec).orElse(null);
    }

    @Override
    public Person update(Person per) {

        if (per.getId() == null) {
            throw new NotFoundIdException();
        }

        Optional<Person> optionalPerson = personRepository.findById(per.getId());
        if (!optionalPerson.isPresent()) {
            throw new NotFoundPersonException();
        }
        Person person = optionalPerson.get();

        String oldFirstName = person.getFirstName();
        String oldMiddleName = person.getMiddleName();
        String oldLastName = person.getLastName();

        String newFirstName = ContragentUtils.toUpperCase(per.getFirstName());
        String newMiddleName = ContragentUtils.toUpperCase(per.getMiddleName());
        String newLastName = ContragentUtils.toUpperCase(per.getLastName());

        String oldFIO = oldFirstName + oldMiddleName + oldLastName;
        String newFIO = newFirstName + newMiddleName + newLastName;

        person.setFirstName(newFirstName);
        person.setMiddleName(newMiddleName);
        person.setLastName(newLastName);

        person.getContragents().forEach(
                contragent -> {
                    String oldSearchName = contragent.getSearchName();
                    String newSearchName = oldSearchName.replace(oldFIO, newFIO);
                    contragent.setSearchName(newSearchName);
                    contragentService.save(contragent);
                }
        );
        return personRepository.save(person);
    }

    @Override
    public void delete(Long id) {

        Optional<Person> optionalPerson = personRepository.findById(id);
        if (!optionalPerson.isPresent()) {
            throw new NotFoundPersonException();
        }
        Person person = optionalPerson.get();

        person.getContragents().forEach(contragent -> {
            contragent.setIsDeleted(true);
            contragentService.save(contragent);
        });
    }

    @Override
    public List<Address> getAddresses(Long id) {

        Optional<Person> optionalPerson = personRepository.findById(id);
        if (!optionalPerson.isPresent()) {
            throw new NotFoundPersonException();
        }

        Person person = optionalPerson.get();

        //берем список записей, которые не помечены как удаленные
        List<Contragent> contragents = person.getContragents().stream()
                .filter(contragent -> !contragent.getIsDeleted())
                .collect(Collectors.toList());
        return contragents.stream()
                //меняем ID адреса на ID контрагента, чтобы на фронте можно было удалить запись
                .map(contragent -> new Address(contragent.getId(),
                        contragent.getAddress().getIndex(),
                        contragent.getAddress().getCountry(),
                        contragent.getAddress().getCity(),
                        contragent.getAddress().getStreet(),
                        contragent.getAddress().getHouseNumber(),
                        contragent.getAddress().getApartmentNumber()
                ))
                .collect(Collectors.toList());
    }
}
