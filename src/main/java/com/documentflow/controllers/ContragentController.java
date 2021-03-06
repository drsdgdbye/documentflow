package com.documentflow.controllers;

import com.documentflow.entities.Address;
import com.documentflow.entities.Contragent;
import com.documentflow.entities.Organization;
import com.documentflow.entities.Person;
import com.documentflow.entities.dto.ContragentDto;
import com.documentflow.entities.dto.ContragentDtoBindAddressAndEmployee;
import com.documentflow.entities.dto.ContragentDtoEmployee;
import com.documentflow.exceptions.BadArgumentException;
import com.documentflow.services.AddressService;
import com.documentflow.services.ContragentService;
import com.documentflow.services.OrganizationService;
import com.documentflow.services.PersonService;
import com.documentflow.exceptions.NotFoundIdException;
import com.documentflow.utils.ContragentUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/contragent")
public class ContragentController {

    @Autowired
    private ContragentService contragentService;

    @Autowired
    private PersonService personService;

    @Autowired
    private AddressService addressService;

    @Autowired
    private OrganizationService organizationService;

    @GetMapping()
    public ModelAndView searchContragent(@RequestParam(value = "searchName") String searchName,
                                         HttpServletRequest request) {

        String redirectUrl = request.getHeader("referer");
        ModelAndView modelAndView = createDefaultModelAndView(redirectUrl);

        if (StringUtils.isNotEmpty(searchName)) {
            modelAndView.addObject("contragents", contragentService.searchContragents(searchName));
        }
        return modelAndView;
    }
    private ModelAndView createDefaultModelAndView(String url) {
        return new ModelAndView(url);
    }

    @GetMapping("/add")
    public String getPageToAdd() {
        return "contragent_edit";
    }

    @GetMapping("/edit")
    public String getPageToEdit() {
        return "contragent_edit";
    }

    @PostMapping("/add")
    @ResponseBody
    public List<Contragent> addNewContragent(@RequestBody ContragentDto contragentDto) {

        if (ContragentUtils.isEmpty(contragentDto.getParameters())) {
            throw new IllegalArgumentException("Не заполнены основные параметры контрагента");
        }
        return contragentService.save(contragentDto);
    }

    @GetMapping("/edit/person")
    @ResponseBody
    public List<Person> getPerson(@RequestParam(name = "first_name", required = false) String firstName,
                                  @RequestParam(name = "middle_name", required = false) String middleName,
                                  @RequestParam(name = "last_name") String lastName) {

        if (StringUtils.isEmpty(lastName)) {
            throw new IllegalArgumentException("Last name is empty");
        }
        return personService.findAll(firstName.toUpperCase(), middleName.toUpperCase(), lastName.toUpperCase());
    }

    @PostMapping("/edit/person")
    @ResponseBody
    public Person editPerson(@Valid @RequestBody Person person) {
        return personService.update(person);
    }

    @DeleteMapping("/edit/person/{id:[\\d]+}")
    @ResponseStatus(HttpStatus.OK)
    public void deletePerson(@PathVariable("id") Long id) {
        personService.delete(id);
    }

    @DeleteMapping("/edit/person/address/{id:[\\d]+}")
    @ResponseStatus(HttpStatus.OK)
    public void deletePersonAddress(@PathVariable("id") Long id) {
        contragentService.delete(id);
    }

    @PostMapping("edit/person/address")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Address addNewAddressToPerson(@Valid @RequestBody Address address) {

        if (ObjectUtils.isEmpty(address.getId())) {
            throw new NotFoundIdException();
        }
        //ВНИМАНИЕ. В полученном объекте типа Address в поле ID хранится ID объекта типа Person
        Long idPerson = address.getId();
        address.setId(0L);

        Address findAddress = addressService.strongFind(address);
        if(findAddress != null) {
            return findAddress;
        } else {
            return contragentService.bindAddressWithPerson(idPerson, address);
        }
    }

    @GetMapping("/edit/person/{id:[\\d]+}/address")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<Address> getAddressToPerson(@PathVariable("id") Long id) {
        return personService.getAddresses(id);
    }

    @GetMapping("/edit/address")
    @ResponseBody
    public List<Address> getAddress(@RequestParam(name = "post_index", required = false) String postIndex,
                                    @RequestParam(name = "country") String country,
                                    @RequestParam(name = "city") String city,
                                    @RequestParam(name = "street") String street,
                                    @RequestParam(name = "house_number", required = false) String houseNumber,
                                    @RequestParam(name = "apartrment_number", required = false) String apartrmentNumber) {

        if (StringUtils.isEmpty(country)) {
            throw new BadArgumentException("Country is empty");
        }
        if (StringUtils.isEmpty(city)) {
            throw new BadArgumentException("City is empty");
        }
        if (StringUtils.isEmpty(street)) {
            throw new BadArgumentException("Street is empty");
        }
        return addressService.findAll(postIndex, country.toUpperCase(), city.toUpperCase(), street.toUpperCase(), houseNumber, apartrmentNumber);
    }

    @PostMapping("/edit/address")
    @ResponseBody
    public Address editAddress(@Valid @RequestBody Address address) {
        return addressService.update(address);
    }

    @DeleteMapping("/edit/address/{id:[\\d]+}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteAddress(@PathVariable("id") long id) {
        addressService.delete(id);
    }

    @GetMapping("/edit/company")
    @ResponseBody
    public List<Organization> getOrganization(@RequestParam(name = "name_company") String nameCompany) {

        if (StringUtils.isEmpty(nameCompany)) {
            throw new BadArgumentException("Company name is empty");
        }
        return organizationService.findAll(nameCompany);
    }

    @GetMapping("/edit/company/{id:[\\d]+}/address")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<Address> getAddressToCompany(@PathVariable("id") Long id) {
        return organizationService.getAddresses(id);
    }

    @PostMapping("edit/company/address")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Address addNewAddressToCompany(@Valid @RequestBody Address address) {

        if (ObjectUtils.isEmpty(address.getId())) {
            throw new NotFoundIdException();
        }
        //ВНИМАНИЕ. В полученном объекте типа Address в поле ID хранится ID объекта типа Organization
        Long idOrganization = address.getId();
        address.setId(0L);

        Address findAddress = addressService.strongFind(address);
        if(findAddress != null) {
            return findAddress;
        } else {
            return contragentService.bindAddressWithOrganization(idOrganization, address);
        }
    }

    @PostMapping("edit/company/employee")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ContragentDtoEmployee addNewEmployeeToCompany(@Valid @RequestBody ContragentDtoEmployee employee) {

        if (ObjectUtils.isEmpty(employee.getId())) {
            throw new NotFoundIdException();
        }
        //ВНИМАНИЕ. В полученном объекте типа ContragentDtoEmployee в поле ID хранится ID объекта типа Organization
        Long idOrganization = Long.valueOf(employee.getId());
        return contragentService.bindEmployeeWithOrganization(idOrganization, employee);
    }

    @PostMapping("edit/company/employee_and_address")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ContragentDtoBindAddressAndEmployee addNewEmployeeAndAddressToCompany(@Valid @RequestBody ContragentDtoBindAddressAndEmployee addressAndEmployee) {

        if (ObjectUtils.isEmpty(addressAndEmployee.getId())) {
            throw new NotFoundIdException();
        }
        return contragentService.bindEmployeeWithAddress(addressAndEmployee);
    }


    @GetMapping("/edit/company/{id:[\\d]+}/employee")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<ContragentDtoEmployee> getEmployeeToCompany(@PathVariable("id") Long id) {
        return organizationService.getEmployees(id);
    }

    @DeleteMapping("/edit/company/address/{id:[\\d]+}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteCompanyAddress(@PathVariable("id") Long id) {
        contragentService.delete(id);
    }

    @DeleteMapping("/edit/company/employee/{id:[\\d]+}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteCompanyEmployee(@PathVariable("id") Long id) {
        contragentService.delete(id);
    }

    @PostMapping("/edit/company")
    @ResponseBody
    public Organization editOrganization(@Valid @RequestBody Organization organization) {
        return organizationService.update(organization);
    }

    @DeleteMapping("/edit/company/{id:[\\d]+}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteOrganization(@PathVariable("id") long id) {
        organizationService.delete(id);
    }

    @GetMapping("/edit/employee")
    @ResponseBody
    public List<ContragentDtoEmployee> getEmployee(@RequestParam(name = "first_name", required = false) String firstName,
                                                   @RequestParam(name = "middle_name", required = false) String middleName,
                                                   @RequestParam(name = "last_name") String lastName,
                                                   @RequestParam(name = "position", required = false) String position) {

        if (StringUtils.isEmpty(lastName)) {
            throw new BadArgumentException("Last name is empty");
        }

        //the order of arguments is important
        String searchString = ContragentUtils.createSearchName(firstName, middleName, lastName, position);
        List<Contragent> contragents = contragentService.searchContragents(searchString);
        return contragents.stream()
                .filter(contragent -> contragent.getOrganization() != null)
                .map(contragent -> {
                    return new ContragentDtoEmployee(contragent.getId().toString(),
                            contragent.getPerson().getFirstName(),
                            contragent.getPerson().getMiddleName(),
                            contragent.getPerson().getLastName(),
                            contragent.getPersonPosition()
                    );
                })
                .collect(Collectors.toList());
    }

    @PostMapping("/edit/employee")
    @ResponseBody
    public Contragent editEmployee(@RequestBody ContragentDtoEmployee employee) {

        if (ObjectUtils.isEmpty(employee.getId())) {
            throw new BadArgumentException("ID is empty");
        }
        if (StringUtils.isEmpty(employee.getLastName())) {
            throw new BadArgumentException("Last name is empty");
        }
        return contragentService.updateEmployee(employee);
    }

    @DeleteMapping("/edit/employee/{id:[\\d]+}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteEmployee(@PathVariable("id") long id) {
        contragentService.delete(id);
    }
}
