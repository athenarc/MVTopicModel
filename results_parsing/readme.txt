This document outlines the results of the topic modelling run.

Basic Terms
-----------
Topic: a semantic thematic unit / concept, describing and illustrating structure among the documents of interest.
       Topics are described by distributions of tokens (e.g. words), and documents are described by distributions of topics.
Token: an informative unit in the document collection (most commonly words).
Modality: an information channel which topic modelling accounts for. Tokens differ per modality,
          e.g. text words (word tokens), text phrases (phrase tokens), MESH ontology (MESH-term tokens), dbpedia ontology (dbpedia concept tokens), etc.


The topic modelling result files 
--------------------------------

1) File: topics_brief.csv:
This file shows basic topic information, in terms of an overall topic weight and with topic component information (with respect to per-modality tokens).
Its columns are:

topicid:     The numeric topic id
topicweight: The overall weight of the topic
modality:    The modality the tokens belong to
concepts:    Prominent tokens for the modality and topic, sorted by descending importance.


2) File: doc_topic.csv
This file shows a ranking of strong document-topic associations, per topic. Its columns are:

topicid: The numeric topic id
id:      The document id
round:   Rounded topic weight for that document, thresholded at 0.6.
title:   The document title.


3) File: topics_detailed.csv
This file shows detailed information with respect to topics, modalities and tokens. Its columns are:

topicid:         The numeric topic id
topicweight:     The overall weight of the topic
totaltokens:     The total number of tokens for the topic (across modalities)
itemtype:        The modality numeric id
modality:        The modality name
concept:         The token name 
item:            Alternative token name (where applicable)
counts:          The token count for that topic
discrweight:     A estimate of the discriminativeness of the token for the topic
weightedcounts:  The weighted token count, in terms of discriminativeness
dbpedia_label:   Additional information for the "dbpedia" modality (omitted)
dbpedia_icd10:   ....
dbpedia_mesh:    ....
dbpedia_type:    ....
experimentid:    The id of the experiment
