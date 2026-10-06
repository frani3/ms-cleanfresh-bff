package com.cleanfresh.ms_cleanfresh_bff.service;

import com.cleanfresh.ms_cleanfresh_bff.dto.AuditEntryResponse;
import com.cleanfresh.ms_cleanfresh_bff.repository.AuditoriaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;

    public AuditoriaService(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    public List<AuditEntryResponse> getAll() {
        return auditoriaRepository.findAll();
    }
}
