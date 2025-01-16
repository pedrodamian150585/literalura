package com.alura.literalura.principal;

import com.alura.literalura.service.LibroService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Principal {

    @Autowired
    private LibroService libroService;


}
