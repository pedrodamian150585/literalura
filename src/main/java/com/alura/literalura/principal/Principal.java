package com.alura.literalura.principal;

import com.alura.literalura.dto.AutorDTO;
import com.alura.literalura.dto.LibroDTO;
import com.alura.literalura.dto.RespuestaLibrosDTO;
import com.alura.literalura.model.Autor;
import com.alura.literalura.model.Libro;
import com.alura.literalura.service.AutorService;
import com.alura.literalura.service.ConsumoAPI;
import com.alura.literalura.service.ConvierteDatos;
import com.alura.literalura.service.LibroService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Clase principal que maneja el menú de interacción con el usuario.
 * Esta clase proporciona opciones para buscar libros, listar libros registrados, listar autores registrados,
 * listar autores vivos en un año específico y listar libros por idioma.
 */
@Component
public class Principal {

    @Autowired
    private LibroService libroService;

    @Autowired
    private AutorService autorService;

    @Autowired
    private ConsumoAPI consumoAPI;

    @Autowired
    private ConvierteDatos convierteDatos;

    private static final String BASE_URL = "https://gutendex.com/books/";

    public void mostrarMenu() {
        Scanner scanner = new Scanner(System.in);
        int opcion = -1;

        do {
            System.out.println("""
                    **************************************
                    Bienvenidos a LITERALURA
                    
                    1. Buscar libro por título
                    2. Listar libros registrados
                    3. Listar autores registrados
                    4. Listar autores vivos en un año específico
                    5. Listar libros por idioma
                    6. Salir""");

            opcion = leerEntero(scanner, "Seleccione una opción: ");

            switch (opcion) {
                case 1:
                    System.out.print("Ingrese el título del libro: ");
                    String titulo = scanner.nextLine();
                    buscarLibroPorTitulo(titulo);
                    break;
                case 2:
                    listarLibrosRegistrados();
                    break;
                case 3:
                    listarAutoresRegistrados();
                    break;
                case 4:
                    int ano = leerEntero(scanner, "Ingrese el año vivo de autor(es) que desea buscar: ");
                    listarAutoresVivosEnAno(ano);
                    break;
                case 5:
                    System.out.println("Ingrese el idioma:");
                    System.out.println("es");
                    System.out.println("en");
                    System.out.println("fr");
                    System.out.println("pt");
                    String idioma = scanner.nextLine();
                    listarLibrosPorIdioma(idioma);
                    break;
                case 0:
                    System.out.println("Saliendo...");
                    break;
                default:
                    System.out.println("Opción no válida. Intente de nuevo.");
            }
        } while (opcion != 0);

        scanner.close();
    }

    private int leerEntero(Scanner scanner, String mensaje) {
        int numero = -1;
        while (true) {
            System.out.print(mensaje);
            if (scanner.hasNextInt()) {
                numero = scanner.nextInt();
                scanner.nextLine(); // Consumir el salto de línea
                break;
            } else {
                System.out.println("Entrada no válida. Por favor, introduzca un número.");
                scanner.next(); // Descartar la entrada no válida
            }
        }
        return numero;
    }

    private void buscarLibroPorTitulo(String titulo) {
        try {
            String encodedTitulo = URLEncoder.encode(titulo, StandardCharsets.UTF_8);
            String json = consumoAPI.obtenerDatos(BASE_URL + "?search=" + encodedTitulo);
            RespuestaLibrosDTO respuestaLibrosDTO = convierteDatos.obtenerDatos(json, RespuestaLibrosDTO.class);
            List<LibroDTO> librosDTO = respuestaLibrosDTO.getLibros();
            if (librosDTO.isEmpty()) {
                System.out.println("Libro no encontrado en la API");
            } else {
                boolean libroRegistrado = false;
                for (LibroDTO libroDTO : librosDTO) {
                    if (libroDTO.getTitulo().equalsIgnoreCase(titulo)) {
                        Optional<Libro> libroExistente = libroService.obtenerLibroPorTitulo(titulo);
                        if (libroExistente.isPresent()) {
                            System.out.println("Detalle: Clave (titulo)=(" + titulo + ") ya existe");
                            System.out.println("No se puede registrar el mismo libro más de una vez");
                            libroRegistrado = true;
                            break;
                        } else {
                            Libro libro = new Libro();
                            libro.setTitulo(libroDTO.getTitulo());
                            libro.setIdioma(libroDTO.getIdiomas().get(0));
                            libro.setNumeroDescargas(libroDTO.getNumeroDescargas());

                            AutorDTO primerAutorDTO = libroDTO.getAutores().get(0);
                            Autor autor = autorService.obtenerAutorPorNombre(primerAutorDTO.getNombre())
                                    .orElseGet(() -> {
                                        Autor nuevoAutor = new Autor();
                                        nuevoAutor.setNombre(primerAutorDTO.getNombre());
                                        nuevoAutor.setAnoNacimiento(primerAutorDTO.getAnoNacimiento());
                                        nuevoAutor.setAnoFallecimiento(primerAutorDTO.getAnoFallecimiento());
                                        return autorService.crearAutor(nuevoAutor);
                                    });

                            libro.setAutor(autor);
                            libroService.crearLibro(libro);
                            System.out.println("Libro registrado: " + libro.getTitulo());
                            mostrarDetallesLibro(libroDTO);
                            libroRegistrado = true;
                            break;
                        }
                    }
                }
                if (!libroRegistrado) {
                    System.out.println("No se encontró un libro exactamente con el título '" + titulo + "' en la API");
                }
            }
        } catch (Exception e) {
            System.out.println("Error al obtener datos de la API: " + e.getMessage());
        }
    }

    private void listarLibrosRegistrados() {
        libroService.listarLibros().forEach(libro -> {
            System.out.println("------LIBRO--------");
            System.out.println("Título: " + libro.getTitulo());
            System.out.println("Autor: " + (libro.getAutor() != null ? libro.getAutor().getNombre() : "Desconocido"));
            System.out.println("Idioma: " + libro.getIdioma());
            System.out.println("Número de descargas: " + libro.getNumeroDescargas());
        });
    }

    private void listarAutoresRegistrados() {
        autorService.listarAutores().forEach(autor -> {
            System.out.println("-------AUTOR-------");
            System.out.println("Autor: " + autor.getNombre());
            System.out.println("Fecha de nacimiento: " + autor.getAnoNacimiento());
            System.out.println("Fecha de fallecimiento: " + (autor.getAnoFallecimiento() != null ? autor.getAnoFallecimiento() : "Desconocido"));
            String libros = autor.getLibros().stream()
                    .map(Libro::getTitulo)
                    .collect(Collectors.joining(", "));
            System.out.println("Libros: [ " + libros + " ]");
        });
    }

    private void listarAutoresVivosEnAno(int ano) {
        List<Autor> autoresVivos = autorService.listarAutoresVivosEnAno(ano);
        if (autoresVivos.isEmpty()) {
            System.out.println("No se encontraron autores vivos en el año " + ano);
        } else {
            autoresVivos.forEach(autor -> {
                System.out.println("-------AUTOR-------");
                System.out.println("Autor: " + autor.getNombre());
                System.out.println("Fecha de nacimiento: " + autor.getAnoNacimiento());
                System.out.println("Fecha de fallecimiento: " + (autor.getAnoFallecimiento() != null ? autor.getAnoFallecimiento() : "Desconocido"));
                System.out.println("Libros: " + autor.getLibros().size());
            });
        }
    }

    private void listarLibrosPorIdioma(String idioma) {
        if ("es".equalsIgnoreCase(idioma) || "en".equalsIgnoreCase(idioma) || "fr".equalsIgnoreCase(idioma) || "pt".equalsIgnoreCase(idioma)) {
            libroService.listarLibrosPorIdioma(idioma).forEach(libro -> {
                System.out.println("------LIBRO--------");
                System.out.println("Título: " + libro.getTitulo());
                System.out.println("Autor: " + (libro.getAutor() != null ? libro.getAutor().getNombre() : "Desconocido"));
                System.out.println("Idioma: " + libro.getIdioma());
                System.out.println("Número de descargas: " + libro.getNumeroDescargas());
            });
        } else {
            System.out.println("Idioma no válido. Intente de nuevo.");
        }
    }

    private void mostrarDetallesLibro(LibroDTO libroDTO) {
        System.out.println("------LIBRO--------");
        System.out.println("Título: " + libroDTO.getTitulo());
        System.out.println("Autor: " + (libroDTO.getAutores().isEmpty() ? "Desconocido" : libroDTO.getAutores().get(0).getNombre()));
        System.out.println("Idioma: " + libroDTO.getIdiomas().get(0));
        System.out.println("Número de descargas: " + libroDTO.getNumeroDescargas());
    }
}