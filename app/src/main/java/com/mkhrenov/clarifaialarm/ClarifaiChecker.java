package com.mkhrenov.clarifaialarm;


import clarifai2.api.ClarifaiBuilder;
import clarifai2.api.ClarifaiClient;
import clarifai2.dto.input.ClarifaiInput;
import clarifai2.dto.model.output.ClarifaiOutput;
import clarifai2.dto.prediction.Concept;

import java.io.File;
import java.util.List;

class ClarifaiChecker {
    private ClarifaiClient client;

    ClarifaiChecker() {
        client = new ClarifaiBuilder("eb1ba82793454a1fba4be27bcbcf49c2").buildSync();
    }


    boolean imageContains(File image, String query) {
        final List<ClarifaiOutput<Concept>> predictionResults =
                client.getDefaultModels().generalModel().predict()
                        .withInputs(ClarifaiInput.forImage(image))
                        .executeSync()
                        .get();

        for (ClarifaiOutput<Concept> result : predictionResults) {
            for (Concept datum : result.data()) {
                if(datum.name().equals(query.toLowerCase()))
                    return true;
            }
        }

        return false;
    }

}
