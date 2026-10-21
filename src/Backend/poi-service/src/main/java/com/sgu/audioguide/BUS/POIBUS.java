package com.sgu.audioguide.BUS;

import com.sgu.audioguide.DAO.POIDAO;
import com.sgu.audioguide.DTO.CreatePOIRequestDTO;
import com.sgu.audioguide.DTO.POIDTO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
}