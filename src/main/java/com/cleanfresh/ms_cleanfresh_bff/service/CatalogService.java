package com.cleanfresh.ms_cleanfresh_bff.service;

import com.cleanfresh.ms_cleanfresh_bff.dto.ServiceResponse;
import com.cleanfresh.ms_cleanfresh_bff.repository.CatalogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CatalogService {

    private final CatalogRepository catalogRepository;

    public CatalogService(CatalogRepository catalogRepository) {
        this.catalogRepository = catalogRepository;
    }

    public List<ServiceResponse> getAll() {
        return catalogRepository.findAll();
    }

    public ServiceResponse getById(Long id) {
        return catalogRepository.findById(id);
    }

    public List<ServiceResponse> getDisponibles() {
        return catalogRepository.findDisponibles();
    }
}
