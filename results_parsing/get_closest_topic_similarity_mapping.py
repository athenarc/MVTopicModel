import psycopg2 as pg
import numpy as np
from numpy import dot
from numpy.linalg import norm
import itertools
import pandas as pd
from collections import Counter

connection = pg.connect(user="user",
                        password="password",
                        host="localhost",
                        port=0000,
                        database="dbname")
cursor = connection.cursor()
q = "select topicvector.topicid, columnid, weight, topicvector.experimentid from topicvector inner join topic_curation_details on topicvector.topicid = topic_curation_details.topicid and topicvector.experimentid = topic_curation_details.experimentid where coherence in ('ok','good') and topicvector.experimentid like 'June%'"
cursor.execute(q)

junetopics = {}
for element in cursor.fetchall():
    t, c, w, _ = element
    if t not in junetopics:
        junetopics[t] = np.zeros((200,),np.float32)
    junetopics[t][c] = w
print("Got {} june topics".format(len(junetopics)))


q = "select * from topicvector where experimentid like 'HEALTH%'"
cursor.execute(q)

octtopics = {}
for element in cursor.fetchall():
    t, c, w, _ = element
    if t not in octtopics:
        octtopics[t] = np.zeros((200,),np.float32)
    octtopics[t][c] = w
print("Got {} oct topics".format(len(octtopics)))

jtlist = sorted(junetopics)
otlist = sorted(octtopics)

source, dest = otlist, jtlist
sourcetopics, desttopics = octtopics, junetopics
sourcecol, destcol  = "oct", "june"
keep_top = min(len(dest), 500)

all_similarities = []

similarities = {}
mappings = {}
# get most similar oct topic, for each june topic
for stop in source:
    sims = []
    print("Checking source topic index", stop)
    for dtop in dest:
        st, dt = sourcetopics[stop], desttopics[dtop]
        sims.append(dot(st, dt)/ (norm(st) * norm(dt)))

    all_similarities.append(sims)

    # order similarity values and their indexes
    sims_indexes = zip(list(range(len(sims))), sims)
    sorted_sims_indexes = sorted(sims_indexes, key=lambda x: x[1], reverse=True)
    sorted_sims_indexes = sorted_sims_indexes[:keep_top]
    # maxsim = max(sims)
    # maxtop = sims.index(maxsim)
    mappings[stop] = [dest[s[0]] for s in sorted_sims_indexes]
    similarities[stop] =  [s[1] for s in sorted_sims_indexes]

print("Mapped topics:",len(similarities))
print("Min/max sim:",min(similarities.values()), max(similarities.values()))
res = pd.DataFrame()
res[sourcecol] = source
for k in range(keep_top):
    column = []
    for x in source:
        column.append(mappings[x][k])
    res[destcol + "_top_" + str(k+1)] = column

    res["similarity" + "_top_" + str(k+1)] = [similarities[x][k] for x in source]
res.to_csv("from_{}_to_{}_closest_mapping_top_{}_curation_considered.csv".format(sourcecol, destcol, keep_top),index=None)

import ipdb; ipdb.set_trace()
pd.DataFrame(all_similarities, index=["{}{}".format(sourcecol,i) for i in source], columns=["{}{}".format(destcol,j) for j in dest]).to_csv("all_similarities.csv")
