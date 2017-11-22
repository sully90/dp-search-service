#!/usr/bin/python
import os

def download_model(model_name):
    '''
    Downloads a given model binary
    '''
    print "Downloading ", model_name
    cmd = "wget http://opennlp.sourceforge.net/models-1.5/%s" % model_name

    os.system(cmd)

if __name__ == "__main__":
    model_names = ["en-ner-dates.bin", "en-ner-locations.bin", "en-ner-money.bin", \
        "en-ner-organization.bin", "en-ner-percentage.bin", "en-ner-persons.bin", "en-ner-time.bin", \
        "en-sent.bin", "en-token.bin"]

    for model_name in model_names:
        if (os.path.isfile("./%s" % model_name) is False):
            download_model(model_name)
