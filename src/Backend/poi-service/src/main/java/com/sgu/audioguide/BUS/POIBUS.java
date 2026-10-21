package com.sgu.audioguide.BUS;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgu.audioguide.DAO.POIDAO;
import com.sgu.audioguide.DTO.CreatePOIRequestDTO;
import com.sgu.audioguide.DTO.POIDTO;
import com.sgu.audioguide.DTO.UpdatePOIRequestDTO;

@Service
public class POIBUS {

    private final POIDAO poiDAO;

    public POIBUS(POIDAO poiDAO) {
        this.poiDAO = poiDAO;
    }

    public List<POIDTO> getVisiblePOIs() {
        return poiDAO.findVisiblePOIs();
    }

    @Transactional
    public Long createPOI(CreatePOIRequestDTO request) {
        if (!poiDAO.areaExists(request.areaId())) {
            throw new IllegalArgumentException(
                    "Khu vực không tồn tại"
            );
        }

        if (poiDAO.existsByCode(request.poiCode().trim())) {
            throw new DuplicateKeyException(
                    "Mã POI đã tồn tại"
            );
        }

        return poiDAO.insert(request);
    }

    @Transactional
    public void updatePOI(Long id, UpdatePOIRequestDTO request) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "ID POI không hợp lệ"
            );
        }

        if (!poiDAO.existsById(id)) {
            throw new NoSuchElementException(
                    "POI không tồn tại"
            );
        }

        if (!poiDAO.areaExists(request.areaId())) {
            throw new IllegalArgumentException(
                    "Khu vực không tồn tại"
            );
        }

        if (poiDAO.existsByCodeExcludingId(
                request.poiCode().trim(), id
        )) {
            throw new DuplicateKeyException(
                    "Mã POI đã tồn tại"
            );
        }

        int updatedRows = poiDAO.update(id, request);

        if (updatedRows == 0) {
            throw new NoSuchElementException(
                    "POI không còn tồn tại để cập nhật"
            );
        }
    }

    @Transactional
    public void updateVisibility(Long id, boolean visible) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "ID POI không hợp lệ"
            );
        }

        int updatedRows = poiDAO.updateVisibility(id, visible);

        if (updatedRows == 0 && !poiDAO.existsById(id)) {
            throw new NoSuchElementException(
                    "POI không tồn tại"
            );
        }
    }
}
