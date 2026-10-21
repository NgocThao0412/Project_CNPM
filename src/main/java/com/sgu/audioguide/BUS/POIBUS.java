package com.sgu.audioguide.BUS;

import com.sgu.audioguide.DAO.POIDAO;
import com.sgu.audioguide.DTO.POIDTO;
import org.springframework.stereotype.Service;

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
}