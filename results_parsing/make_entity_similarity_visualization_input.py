import itertools
import json
from collections import Counter
from os.path import exists

import numpy as np
import pandas as pd
import psycopg2 as pg
from numpy import dot
from numpy.linalg import norm


def get_raw_similarities():

    if exists("credentials.json"):
        with open("credentials.json") as f:
            creds = json.load(f)
    connection = pg.connect(**creds)
    cursor = connection.cursor()

    # get topics of interest
    
    q = """
    inner join topic_curation_details on topicvector.topicid = topic_curation_details.topicid and topicvector.experimentid = topic_curation_details.experimentid where coherence in ('ok','good') and topicvector.experimentid like 'June%'
    """
    q = " select entityid1, entityid2, similarity from entitysimilarity where entitytype = 'Project' and experimentid like 'June%' limit 100"
    cursor.execute(q)
    res = {}
    for e1, e2, sim in cursor.fetchall():
        res[(e1, e2)] = sim
    print("Got {} similarity pairs.".format(len(res)))
    return res, []


# filter pairs of the form: 
# with a sim. threshold, to force graph appropriate input.
# color information represents a category to determine coloring in the graph
# pairs: {(id1, id2): similarity, ... }
# color_information: {id: color_info}
def similarity_pairs_to_force_graph(pairs, threshold, information):
    all_nodes = set([p for pp in pairs for p in pp])
    # log all nodes
    nodes = []
    for node in all_nodes:
        ddct = {"id": node}
        if node in information:
            ddct["user"] = information[node]
        nodes.append(ddct)
    # log all links
    links = []
    for p1, p2 in pairs:
        links.append({"source": p1, "target": p2})
    return {"nodes": nodes, "links": links}


# filter pairs of the form:
# with a sim. threshold, to force graph appropriate input.
# color information represents a category to determine coloring in the graph
# pairs: {(id1, id2): similarity, ... }
# size_information: {id: size}
def similarity_pairs_to_chord(pairs, threshold, information):
    # log all nodes
    nodes = {}
    for p1, p2 in pairs:
        if p1 not in nodes:
            nodes[p1] = {"name": p1, "imports": []}
            if p1 in information:
                nodes[p1]["size"] = information[p1]
        if p2 not in nodes:
            nodes[p2] = {"name": p2, "imports": []}
            if p2 in information:
                nodes[p2]["size"] = information[p2]
        # add edge p1 -> p2
        if p1 in nodes[p2]["imports"]:
            print("Already an incoming edge from {} into {}!".format(p1, p2))
            exit(1)
        nodes[p2]["imports"].append(p1)
    return list(nodes.values())


if __name__ == '__main__':
    res, information = get_raw_similarities()
    threshold = 0.4
    res = {p for p in res if res[p] >= threshold}
    if not res:
        print("Threshold too high, no results.")
        exit(1)
    fg = similarity_pairs_to_force_graph(res, threshold, information)
    with open("force_graph.json", "w") as f:
        json.dump(fg, f)

    ch = similarity_pairs_to_chord(res, threshold, information)
    with open("chord.json", "w") as f:
        json.dump(ch, f)
