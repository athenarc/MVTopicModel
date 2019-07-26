import pandas as pd
from unidecode import unidecode

""" PRocess doc-topic associations to easify (TM) curation"""

# docs = "doc_topic.csv  topics_brief.csv  topics_detailed.csv".split()
# docs = "doc_topic.csv".split()
doc = "doc_topic_threshp5.csv.cleaned.csv"
count_threshold = 20

rows_to_keep = []
counts = {}

print(doc)
dat=pd.read_csv(doc)

for i, row in dat.iterrows():
    topicid = row['topicid']
    if topicid not in counts:
        counts[topicid] = 0
    if counts[topicid] >= count_threshold:
        continue
    counts[topicid] += 1
    rows_to_keep.append(i)
    print("Keeping doc {}/{}: {} for topic {}".format(counts[topicid], count_threshold, row['id'], topicid))

print("Kept {} out of {} rows".format(len(rows_to_keep), len(dat)))
newdat = dat.iloc[rows_to_keep]
newdat.to_csv(doc + ".prepared_for_curation.csv", index=False)
