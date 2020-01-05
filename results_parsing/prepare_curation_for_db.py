import pandas as pd
import collections
path = "/home/nik/athena/junerun/curation_results/topics_brief labelled_formatcorrected.csv"
# import chardet
# rawdata = open(path, rb).read()
# result = chardet.detect(rawdata)
# charenc = result[encoding]
# print(charenc)

# remove numeric category prefix
def drop_numeric(category):
    category = category.strip()
    cat = category.split(maxsplit=1)[1] if category.split()[0][-1] == "." and all(
        x.isdigit() for x in category.split()[0][:-1]) else category
    return cat

x = pd.read_csv(path, encoding="Windows-1252",sep=";")

topics = {}
for rowidx, row in enumerate(x.iterrows()):
    topicid, topicweight, modality, concepts, topic_label, coherence_assessment, \
     technical_issues, fewpubs, comments, category, possible_categories = row[1].values

    # preprocess
    topic_label, category = [x.strip().replace("'", "''") for x in [topic_label, category]]
    category = drop_numeric(category)
    if topicid not in topics:
        topics[topicid] = {}
        for ex in ["label", "category"]:
            topics[topicid][ex] = None
    # check no inconsistencies exist
    for (key, val) in zip(["label", "category"], [topic_label, category]):
        exval = topics[topicid][key]
        if exval is not None:
            if exval != val:
                print("Existing {} for topicid {} in row {} is [{}] but stored previous value is [{}] !".format(key, topicid, rowidx, exval, val))
                exit(1)
        else:
            topics[topicid][key] = val

orig_cats = [drop_numeric(k) for k in x["Possible categories"].dropna().to_list()]
cats = list(set([t["category"] for t in topics.values()]))
if not set(orig_cats) == set(cats):
    print("Category mismatch")
    print(set(cats) - set(orig_cats))
    print(set(orig_cats) - set(cats))
    exit(1)
print(pd.DataFrame.from_dict(topics, orient='index'))
# write sql
table = "topic_curation_details"
columns ="topicid label category curator experimentid".split()
curator = "PPMI"
experimentid = "JuneRun_PubMed_500T_550IT_7000CHRs_3M_OneWay"

sql = "insert into {}({}) values \n".format(table, ",".join(columns))
values = []
for t in topics:
    l, c = topics[t]["label"], topics[t]["category"]
    values.append("({}, '{}', '{}', '{}', '{}')".format(t, l, c, curator, experimentid))
sql += ",\n".join(values)
sql += "\n"

print(sql)
outfile = "insert_curation_{}_{}.sql".format(experimentid, curator)
print("Writing to", outfile)
with open(outfile, "w") as f:
    f.write(sql)
