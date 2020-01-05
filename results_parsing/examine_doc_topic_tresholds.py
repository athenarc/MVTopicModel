import pandas as pd
import os
data = pd.read_csv("/home/nik/athena/tables_dimitris/doctopic_table/doc_topic_associations.csv")

topic_sample = list(set(data["topicid"].tolist()))[:10]
threshold_range = [x/10 for x in range(2,7)]

print("</html>")
for threshold in threshold_range:
    print("Threshold", threshold,"<br/>")
    dat = data[data["weight"] > threshold]
    for t in topic_sample:
        print(t, dat[dat["topicid"] ==t]["topic_label"].any(),"<br/>")
        dois = dat[dat["topicid"] == t].sort_values("weight")
        dois = dois["doi"].dropna().drop_duplicates()
        if len(dois) == 0:
            print("No documents!<br/>")
        else:
            for doi in dois[:10]:
                url = "https://doi.org/{}".format(doi)
                print("<a href='{}'>{}</a><br/>".format(url, url))
                # os.system("firefox {}".format(url))

print("</html>")

