package com.parking.service;

import com.parking.dao.PlaceDao;
import com.parking.model.Place;
import com.parking.model.StatutPlace;

import java.util.List;

/**
 * Consultation des places pour l'interface (§4.2 CDC).
 */
public class PlaceService {

    private final PlaceDao placeDao;

    public PlaceService() {
        this(new PlaceDao());
    }

    PlaceService(PlaceDao placeDao) {
        this.placeDao = placeDao;
    }

    public List<Place> getAllPlaces() {
        return placeDao.findAll();
    }

    public int countLibres() {
        return placeDao.countByStatut(StatutPlace.LIBRE);
    }

    public int countOccupees() {
        return placeDao.countByStatut(StatutPlace.OCCUPEE);
    }

    public int countTotal() {
        return countLibres() + countOccupees();
    }
}
