package healthy.lifestyle.backend.activity.mental.controller;

import healthy.lifestyle.backend.activity.mental.dto.MentalCreateRequestDto;
import healthy.lifestyle.backend.activity.mental.dto.MentalResponseDto;
import healthy.lifestyle.backend.activity.mental.dto.MentalUpdateRequestDto;
import healthy.lifestyle.backend.activity.mental.service.MentalService;
import healthy.lifestyle.backend.shared.validation.annotation.IdValidation;
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
@RequestMapping("${api.basePath}/${api.version}/mentals")
public class MentalController {
    @Autowired
    MentalService mentalService;

    @Autowired
    AuthUtil authUtil;

    @GetMapping("/default/{mental_id}")
    public ResponseEntity<MentalResponseDto> getDefaultMentalById(
            @PathVariable("mental_id") @IdValidation long mental_id) {
        MentalResponseDto responseDto = mentalService.getMentalById(mental_id, true, null);
        return ResponseEntity.ok(responseDto);
    }

    @GetMapping("/{mental_id}")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<MentalResponseDto> getCustomMentalById(
            @PathVariable("mental_id") @IdValidation long mental_id) {
        Long userId = authUtil.getUserIdFromAuthentication(
                SecurityContextHolder.getContext().getAuthentication());
        MentalResponseDto responseDto = mentalService.getMentalById(mental_id, false, userId);
        return ResponseEntity.ok(responseDto);
    }

    @GetMapping("/all_mentals")
    public ResponseEntity<Page<MentalResponseDto>> getAllMentals(
            @RequestParam(required = false, defaultValue = "title") String sortField,
            @RequestParam(required = false, defaultValue = "ASC") String sortDirection,
            @RequestParam(required = false, defaultValue = "10") int pageSize,
            @RequestParam(required = false, defaultValue = "0") int pageNumber) {
        Long userId = authUtil.getUserIdFromAuthentication(
                SecurityContextHolder.getContext().getAuthentication());
        Page<MentalResponseDto> responseDtoPage =
                mentalService.getMentals(userId, sortField, sortDirection, pageNumber, pageSize);
        return ResponseEntity.ok(responseDtoPage);
    }

    @PatchMapping("/{mentalId}")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<MentalResponseDto> updateCustomMental(
            @PathVariable("mentalId") long mentalId, @Valid @RequestBody MentalUpdateRequestDto requestDto)
            throws NoSuchFieldException, IllegalAccessException {
        Long userId = authUtil.getUserIdFromAuthentication(
                SecurityContextHolder.getContext().getAuthentication());
        MentalResponseDto responseDto = mentalService.updateCustomMental(userId, mentalId, requestDto);
        return ResponseEntity.ok(responseDto);
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<MentalResponseDto> createCustomMental(@Valid @RequestBody MentalCreateRequestDto requestDto) {
        Long userId = authUtil.getUserIdFromAuthentication(
                SecurityContextHolder.getContext().getAuthentication());
        MentalResponseDto responseDto = mentalService.createCustomMental(userId, requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }
}
