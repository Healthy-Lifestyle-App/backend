package healthy.lifestyle.backend.activity.workout.controller;

import healthy.lifestyle.backend.activity.workout.dto.HttpRefCreateRequestDto;
import healthy.lifestyle.backend.activity.workout.dto.HttpRefResponseDto;
import healthy.lifestyle.backend.activity.workout.dto.HttpRefUpdateRequestDto;
import healthy.lifestyle.backend.activity.workout.service.HttpRefService;
import healthy.lifestyle.backend.shared.validation.annotation.DescriptionOptionalValidation;
import healthy.lifestyle.backend.shared.validation.annotation.IdValidation;
import healthy.lifestyle.backend.shared.validation.annotation.TitleOptionalValidation;
import healthy.lifestyle.backend.user.service.AuthUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@Controller
@RequestMapping("${api.basePath}/${api.version}/workouts/httpRefs")
public class HttpRefController {
    @Autowired
    HttpRefService httpRefService;

    @Autowired
    AuthUtil authUtil;

    @PostMapping
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<HttpRefResponseDto> createCustomHttpRef(
            @RequestBody @Valid HttpRefCreateRequestDto requestDto) {
        Long userId = authUtil.getUserIdFromAuthentication(
                SecurityContextHolder.getContext().getAuthentication());
        HttpRefResponseDto responseDto = httpRefService.createCustomHttpRef(userId, requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @GetMapping("/{httpRefId}")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<HttpRefResponseDto> getCustomHttpRefById(@PathVariable @IdValidation long httpRefId) {
        Long userId = authUtil.getUserIdFromAuthentication(
                SecurityContextHolder.getContext().getAuthentication());
        HttpRefResponseDto responseDto = httpRefService.getCustomHttpRefById(userId, httpRefId);
        return new ResponseEntity<>(responseDto, HttpStatus.OK);
    }

    @GetMapping()
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Page<HttpRefResponseDto>> getHttpRefsWithFilter(
            @RequestParam(required = false) Boolean isCustom,
            @RequestParam(required = false) @TitleOptionalValidation(min = 2) String name,
            @RequestParam(required = false) @DescriptionOptionalValidation(min = 2) String description,
            @RequestParam(required = false, defaultValue = "id") String sortField,
            @RequestParam(required = false, defaultValue = "ASC") String sortDirection,
            @RequestParam(required = false, defaultValue = "10") int pageSize,
            @RequestParam(required = false, defaultValue = "0") int pageNumber) {
        Long userId = null;
        if (isCustom == null || isCustom)
            userId = authUtil.getUserIdFromAuthentication(
                    SecurityContextHolder.getContext().getAuthentication());
        Page<HttpRefResponseDto> responseDtoPage = httpRefService.getHttpRefsWithFilter(
                isCustom, userId, name, description, sortField, sortDirection, pageNumber, pageSize);
        return new ResponseEntity<>(responseDtoPage, HttpStatus.OK);
    }

    @GetMapping("/default")
    public ResponseEntity<Page<HttpRefResponseDto>> getDefaultHttpRefsWithFilter(
            @RequestParam(required = false) @TitleOptionalValidation(min = 2) String name,
            @RequestParam(required = false) @DescriptionOptionalValidation(min = 2) String description,
            @RequestParam(required = false, defaultValue = "id") String sortField,
            @RequestParam(required = false, defaultValue = "ASC") String sortDirection,
            @RequestParam(required = false, defaultValue = "10") int pageSize,
            @RequestParam(required = false, defaultValue = "0") int pageNumber) {
        Boolean isCustom = null;
        Long userId = null;
        Page<HttpRefResponseDto> responseDtoPage = httpRefService.getHttpRefsWithFilter(
                isCustom, userId, name, description, sortField, sortDirection, pageNumber, pageSize);
        return new ResponseEntity<>(responseDtoPage, HttpStatus.OK);
    }

    @PatchMapping("/{httpRefId}")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<HttpRefResponseDto> updateCustomHttpRef(
            @PathVariable Long httpRefId, @RequestBody @Valid HttpRefUpdateRequestDto requestDto)
            throws NoSuchFieldException, IllegalAccessException {
        Long userId = authUtil.getUserIdFromAuthentication(
                SecurityContextHolder.getContext().getAuthentication());
        HttpRefResponseDto responseDto = httpRefService.updateCustomHttpRef(userId, httpRefId, requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.OK);
    }

    @DeleteMapping("/{httpRefId}")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<?> deleteCustomHttpRef(@PathVariable @IdValidation long httpRefId) {
        Long authUserId = authUtil.getUserIdFromAuthentication(
                SecurityContextHolder.getContext().getAuthentication());
        httpRefService.deleteCustomHttpRef(authUserId, httpRefId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
