package com.documentflow.controllers;

import com.documentflow.entities.*;
import com.documentflow.entities.DTO.DocInDTO;
import com.documentflow.model.enums.BusinessKeyState;
import com.documentflow.services.*;
import com.documentflow.utils.DocInUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/docs/in")
public class DocInController {

    private DocInService docInService;
    private DocTypeService docTypeService;
    private DepartmentService departmentService;
    private UserService userService;
    private DocInUtils docInUtils;
    private StateService stateService;

    @Autowired
    public DocInController(DocInService docInService, DocTypeService docTypeService,
                           DepartmentService departmentService, UserService userService,
                           DocInUtils docInUtils, StateService stateService) {
        this.docInService = docInService;
        this.docTypeService = docTypeService;
        this.departmentService = departmentService;
        this.userService = userService;
        this.docInUtils = docInUtils;
        this.stateService = stateService;
    }

    @GetMapping()
    public String showIn(
            Model model,
            @RequestParam(value = "currentPage", required = false) Integer currentPage) {
        if (currentPage == null || currentPage < 1) {
            currentPage = 1;
        }
        model.addAttribute("currentPage", currentPage);
        Page<DocIn> page = docInService.findAll(PageRequest.of(currentPage-1,20, Sort.Direction.ASC, "regDate"));
        Page<DocInDTO> pageDTOs = page.map(d -> new DocInDTO(d));
        model.addAttribute("docs", pageDTOs);

        model.addAttribute("docTypes", docTypeService.findAllDocTypes());
        model.addAttribute("departments", departmentService.findAllDepartments());
        return "docIn";
    }

    @ResponseBody
    @RequestMapping("/card/{id}")
    public DocInDTO getCard(@PathVariable("id") Long id, Model model) {
        DocInDTO docIn = new DocInDTO(docInService.findById(id));
//        DocInDTO docIn = new DocInDTO();
//        docIn.setDepartmentId(-1);
//        docIn.setDocTypeId(-1);
//        docIn.setUser(userService.getCurrentUser(1));//Заменить на релаьно авторизованного юзера
//        if (id != null) {
//            docIn = new DocInDTO(docInService.findById(id));
//        }
        return docIn;
    }

    @PostMapping("/card")
    public String registrationDoc(@ModelAttribute(name = "doc") DocInDTO docInDTO) {
        System.out.println(docInDTO.toString());
        DocIn docIn = docInDTO.convertToDocIn(docTypeService.getDocTypeById(docInDTO.getDocTypeId()),
                departmentService.getDepartmentById(docInDTO.getDepartmentId()));
        docIn.setRegNumber(docInUtils.getRegNumber());
        State state = stateService.getStateById(1);//Переключиться на BusinessKey когда он заработает.
//        State state = stateService.getStateByBusinessKey(BusinessKeyState.REGISTRATED.toString());
        docIn.setState(state);
        docInService.save(docIn);
        return "redirect:/docs/in";
    }

    @GetMapping("/del")
    public String delete(Long id) {
        docInService.deleteById(id);
        return "redirect:/docs/in";
    }
}
