package com.example.petcare.data;

import com.example.petcare.data.entities.SymptomTag;

public class DefaultSymptomSeeder {

    public static void seed(AppDatabase database) {
        if (database.symptomTagDao().count() > 0) {
            return;
        }

        insert(database, "Digestive", "Vomiting");
        insert(database, "Digestive", "Diarrhoea");
        insert(database, "Digestive", "Constipation");
        insert(database, "Digestive", "Appetite loss");
        insert(database, "Digestive", "Excessive thirst");

        insert(database, "Skin & coat", "Itching");
        insert(database, "Skin & coat", "Hair loss");
        insert(database, "Skin & coat", "Rash");
        insert(database, "Skin & coat", "Dandruff");
        insert(database, "Skin & coat", "Licking paws");

        insert(database, "Respiratory", "Coughing");
        insert(database, "Respiratory", "Sneezing");
        insert(database, "Respiratory", "Wheezing");
        insert(database, "Respiratory", "Nasal discharge");

        insert(database, "Eyes & ears", "Eye discharge");
        insert(database, "Eyes & ears", "Red eyes");
        insert(database, "Eyes & ears", "Head shaking");
        insert(database, "Eyes & ears", "Ear scratching");

        insert(database, "Musculoskeletal", "Limping");
        insert(database, "Musculoskeletal", "Reluctance to move");
        insert(database, "Musculoskeletal", "Swollen joints");

        insert(database, "Behaviour", "Lethargy");
        insert(database, "Behaviour", "Aggression");
        insert(database, "Behaviour", "Anxiety");
        insert(database, "Behaviour", "Excessive vocalisation");
        insert(database, "Behaviour", "Hiding");
        insert(database, "Behaviour", "Nesting behaviour");
        insert(database, "Behaviour", "Mothering behaviour");

        insert(database, "Urinary", "Frequent urination");
        insert(database, "Urinary", "Straining");
        insert(database, "Urinary", "Blood in urine");

        insert(database, "Reproductive", "Vulval swelling");
        insert(database, "Reproductive", "Bloody discharge");
        insert(database, "Reproductive", "Clear discharge");
        insert(database, "Reproductive", "Milk production");

        insert(database, "Other", "Fever (subjective)");
        insert(database, "Other", "Weight change");
        insert(database, "Other", "Bad breath");
    }

    private static void insert(AppDatabase database, String group, String name) {
        SymptomTag tag = new SymptomTag();
        tag.bodySystem = group;
        tag.name = name;
        tag.customTag = false;
        database.symptomTagDao().insert(tag);
    }
}
