package com.example.crud.controller;

import com.example.crud.dto.UserRequestDTO;
import com.example.crud.dto.ErrorResponseDTO;
import com.example.crud.dto.UserResponseDTO;
import com.example.crud.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.example.crud.dto.PageResponseDTO;
import com.example.crud.exception.InvalidUserQueryException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
@RequestMapping("/users")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "basicAuth")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
@ApiResponse(responseCode = "401", description = "Credenciais ausentes ou inválidas",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
@ApiResponse(responseCode = "403", description = "Permissão insuficiente ou token CSRF ausente/inválido",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @ApiResponse(responseCode = "400", description = "Nome ou e-mail inválido, JSON malformado ou corpo ausente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    @ApiResponse(responseCode = "201", description = "Usuário criado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponseDTO.class)))
    @ApiResponse(responseCode = "409", description = "E-mail já cadastrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponseDTO createUser(@Valid @RequestBody UserRequestDTO dto) {
        return userService.createUser(dto);
    }

    @GetMapping(produces = "application/json")
    @Operation(summary = "Listar usuários com paginação, ordenação e busca por nome",
            description = "Retorna um objeto com content e metadados, inclusive sem parâmetros. "
                    + "A busca é parcial, ignora caixa e preserva acentos. name vazio não filtra. "
                    + "Ao ordenar por name, id na mesma direção resolve empates.")
    @ApiResponse(responseCode = "200", description = "Página de usuários; sem resultados ou além do fim retorna content vazio")
    @ApiResponse(responseCode = "400", description = "Parâmetros inválidos; resposta com status, error, message e timestamp",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class),
                    examples = @ExampleObject(value = "{\"status\":400,\"error\":\"Bad Request\",\"message\":\"size deve estar entre 1 e 100\",\"timestamp\":\"2026-10-21T15:00:00\"}")))
    public PageResponseDTO<UserResponseDTO> getAllUsers(
            @Parameter(description = "Índice da página a partir de zero", schema = @Schema(type = "integer", format = "int32", minimum = "0", defaultValue = "0"))
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Quantidade por página, de 1 a 100", schema = @Schema(type = "integer", format = "int32", minimum = "1", maximum = "100", defaultValue = "10"))
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Um único par campo,direção: id ou name; asc ou desc", example = "name,asc",
                    schema = @Schema(defaultValue = "id,asc"))
            @RequestParam(defaultValue = "id,asc") String sort,
            @Parameter(description = "Busca parcial sem diferenciar caixa; espaços nas pontas são removidos",
                    schema = @Schema(maxLength = 255))
            @RequestParam(required = false) String name,
            HttpServletRequest request) {
        for (String parameter : new String[]{"page", "size", "sort"}) {
            String raw = request.getParameter(parameter);
            if (raw != null && raw.isBlank()) {
                throw new InvalidUserQueryException(parameter + " não pode ser vazio");
            }
        }
        if (page < 0) throw new InvalidUserQueryException("page deve ser maior ou igual a 0");
        if (size < 1 || size > 100) throw new InvalidUserQueryException("size deve estar entre 1 e 100");
        if ((long) page * size > Integer.MAX_VALUE) {
            throw new InvalidUserQueryException("page multiplicado por size excede o limite de offset suportado");
        }
        String[] sorts = request.getParameterValues("sort");
        if ((sorts != null && sorts.length != 1) || !sort.matches("(id|name),(asc|desc)")) {
            throw new InvalidUserQueryException("sort deve conter um único par: id,asc; id,desc; name,asc ou name,desc");
        }
        if (name != null && name.length() > 255) {
            throw new InvalidUserQueryException("name deve ter no máximo 255 caracteres");
        }
        String[] order = sort.split(",");
        Sort.Direction direction = Sort.Direction.fromString(order[1]);
        Sort sorting = Sort.by(direction, order[0]);
        if (order[0].equals("name")) sorting = sorting.and(Sort.by(direction, "id"));
        return userService.getAllUsers(name, PageRequest.of(page, size, sorting));
    }

    @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    @ApiResponse(responseCode = "200", description = "Usuário encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponseDTO.class)))
    @GetMapping("/{id}")
    public UserResponseDTO getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @ApiResponse(responseCode = "400", description = "Nome ou e-mail inválido, JSON malformado ou corpo ausente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    @ApiResponse(responseCode = "200", description = "Usuário atualizado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponseDTO.class)))
    @ApiResponse(responseCode = "409", description = "E-mail já cadastrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    @PutMapping("/{id}")
    public UserResponseDTO putUser(
            @PathVariable Long id,
            @Valid @RequestBody UserRequestDTO dto) {
        return userService.updateUser(id, dto);
    }

    @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDTO.class)))
    @ApiResponse(responseCode = "204", description = "Usuário excluído", content = @Content)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
    }
}
