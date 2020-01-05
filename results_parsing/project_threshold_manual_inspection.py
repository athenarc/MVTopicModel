import os
import random

import pandas as pd

data = pd.read_csv("/home/nik/athena/tables_dimitris/doctopic_table/dimitris_table_doctopic_nomocks_withDOI.csv")
projects = pd.read_csv("/home/nik/athena/tables_dimitris/topic_weights_ecprojects_otherids/data-1571851011548.csv")
prj = pd.read_csv("/home/nik/athena/tables_dimitris/projects_variable_threshold/project_titles.csv")
# projects = pd.merge(projects, )

num_projects_tocheck = 10
alltopics = list(set(data["topicid"].tolist()))
threshold_range = [x/10 for x in range(2,8)]
df = []

max_docs_per_topic = 3
max_tops_per_project = 4

print("</html>")
for threshold in threshold_range:
    num_projects_checked = 0
    print("###############   Threshold", threshold,"<br/>")
    # apply the thresh, keep project rcn ids that survive
    projthresh = projects[projects["alldocs_weight"] > threshold]
    proj = projthresh.sort_values("alldocs_weight")["project_rcnid"].dropna().tolist()
    num_unique, num_all_unique = len(set(proj)), len(set(projects["project_rcnid"].tolist()))
    print("{} / {} unique ec projects -- {}/{} total, survive with threshold".format(num_unique,num_all_unique, len(proj), len(projects)), threshold, "<br />")

    for p in proj:
        # get topic closest to the project-wise threshold
        pdocs = data[data["project_rcnid"] == p]
        ptops = projthresh[projthresh["project_rcnid"] == p]

        project_topics = ptops["topicid"].drop_duplicates().tolist()
        num_project_docs = len(pdocs["doi"].drop_duplicates())
        min_project_weight = ptops.sort_values("alldocs_weight").head(1)["alldocs_weight"].item()

        weights_topics_labels = ptops.sort_values("alldocs_weight")["topicid alldocs_weight label".split()]
        dois_list = {}
        for _, row in weights_topics_labels.iterrows():
            t, w, l = row
            dois_list[(t,w,l)] = []
            # get DOIs for the topic
            dat = pdocs[pdocs["topicid"] ==t]
            if len(dat) == 0:
                continue
            # descending weight
            dat = dat.sort_values("weight", ascending=False)
            # top label dois
            dois = dat["doi"].dropna().drop_duplicates().tolist()[:max_docs_per_topic]
            dois = dat[dat.doi.isin(dois)]
            for _, drow in dois.iterrows():
                dois_list[(t, w, l)].append((drow.doi, drow.weight))

        if any(len(v) > 0 for (k,v) in dois_list.items()):
            print("<br />+ PROJECT {} (#{} for thresh {}), min_surviving_projtop_weight {}, numdocs: {}, alltopics: {} <br />".format(p, num_projects_checked+1, threshold, min_project_weight, num_project_docs, project_topics))
            for top_w_l, dois in dois_list.items():
                top, topicw, label = top_w_l
                if len(dois) ==0: continue
                print(top, label,  "| weight:", topicw, "<br/>")
                for (doi, doiweight) in dois:
                    url = "https://doi.org/{}".format(doi)
                    print("<a href='{}'>{} : {}</a><br/>".format(url, url, doiweight))
                    df.append({"threshold": threshold, "project":p, "project_min_topic_weight": min_project_weight, "projtopics":project_topics, "numprojdocs": num_project_docs, "doc": url, "project_topic_weight": topicw, "label": label, "doctopicweight": doiweight})
            num_projects_checked += 1

        if num_projects_checked > num_projects_tocheck:
            print("Reached {} projects to check.<br/>".format(num_projects_checked-1))
            break


print("</html>")
pd.DataFrame(df).to_csv("output.csv")
