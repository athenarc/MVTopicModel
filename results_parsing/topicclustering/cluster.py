import psycopg2 as pg
from collections import Counter
from itertools import product
from os.path import exists
import yaml
import numpy as np
import pickle
from sklearn.cluster import AgglomerativeClustering

modality_names = {-1: "phrase", 0: "text", 1: "mesh", 2: "dbpedia"}
# thresh for token inclusion in the analysis
token_count_threshold = 20
# show these top k tokens per cluster
cluster_token_topk = 50
do_tfidf = True
expid = "JuneRun_PubMed_500T_550IT_7000CHRs_3M_OneWay"

with open("credentials.yml") as f:
    creds = yaml.load(f)

connection, cursor = None, None
term_set = set()
topics_to_terms = {}

topic_tokens_file = "topic_tokens.pickle"
topics_dense_file = "topic_dense_vectors.pickle"

if exists(topic_tokens_file):
    print("Reading existing topic tokens from {}".format(topic_tokens_file))
    with open(topic_tokens_file, "rb") as f:
        term_set, topics_to_terms = pickle.load(f)
else:
    print("Getting topic/token information from the db")

    try:
        connection = pg.connect(user=creds["user"],
                                password=creds["password"],
                                host=creds["host"],
                                port=creds["port"],
                                database=creds["db"])
        cursor = connection.cursor()
        query_tokens_per_topic = "select * from topicanalysis where experimentid='{}'".format(expid)
        cursor.execute(query_tokens_per_topic)

        print("Parsing topic tokens...")
        for element in cursor.fetchall():
            topicid, itemtype, token, token_count, _, _ = element
            itemtype = modality_names[itemtype]
            if token_count < token_count_threshold:
                continue
            modality_token = "{}_{}".format(itemtype, token)
            term_set.add(modality_token)
            if topicid not in topics_to_terms:
                topics_to_terms[topicid] = {}
            topics_to_terms[topicid][modality_token] = token_count

        print("Done processing")
        with open(topic_tokens_file, "wb") as f:
            pickle.dump([term_set, topics_to_terms], f)

    except (Exception, pg.Error) as error:
        print("Error while connecting to PostgreSQL", error)
        exit(1)

    finally:
        # close
        if(connection):
            cursor.close()
            connection.close()

if exists(topics_dense_file):
    print("Reading topic dense")
    with open(topics_dense_file, "rb") as f:
        dense = pickle.load(f)
else:
    term_list = sorted(list(term_set))
    # make dense
    print("Generating dense")
    if do_tfidf:
        global_weights = {}
        for term in term_list:
            if term not in global_weights:
                global_weights[term] = 0
            for tdict in topics_to_terms.values():
                if term in tdict:
                    global_weights[term] += tdict[term]

    dense = np.zeros((0, len(term_list)),np.float32)
    for i, topicid in enumerate(topics_to_terms):
        print("Dense {}/{}".format(i+1, len(topics_to_terms)))
        topic_vec = [topics_to_terms[topicid][term] if term in topics_to_terms[topicid] else 0 for term in term_list]
        if do_tfidf:
            topic_vec = [topic_vec[i] / global_weights[term] for (i, term) in enumerate(term_list)]
        dense = np.vstack((dense, np.asarray(topic_vec)))
    print("Final dense shape:", dense.shape)

    with open(topics_dense_file, "wb") as f:
        pickle.dump(dense, f)


for aff, link in product(["l1", "l2", "manhattan", "cosine"], ["ward", "complete", "average", "single"]):
    print("Clustering with affinity / linkage:", aff, link)
    try:
        aggl = AgglomerativeClustering(n_clusters=10, affinity=aff, linkage=link)
        clusters = aggl.fit(dense)
        print(aggl.n_clusters)
        print(clusters.labels_)
        print("Cluster freqs:")
        for clu, count in Counter(clusters.labels_).most_common():
            print("{:2d}: {}".format(clu, count))
        with open("clustering.{}.{}.pickle".format(aff, link), "wb") as f:
            pickle.dump([aggl, clusters], f)
        # get the most frequent toks in the topic cluster
        cluster_tokens = {}
        for cluster in set(clusters.labels_):
            toks = {}
            cluster_topics = np.where(clusters.labels_ == cluster)[0]
            for topic in cluster_topics:
                for token, count in topics_to_terms[topic].items():
                    if token not in toks:
                        toks[token] = 0
                    toks[token] += count
            # sort by freq
            toks = list(sorted(toks.items(), key=lambda x: x[1]))
            # keep only top k names
            cluster_tokens[cluster] = [x[0] for x in toks[:cluster_token_topk]]

        # write tokens
        with open("clustering.tokens.{}.{}.txt".format(aff, link), "w") as f:
            for cluster, toks in cluster_tokens.items():
                f.write("{}: {}\n".format(cluster, ",".join(toks)))

    except ValueError:
        # incompatible affinity / linkage combo
        print("Skipping incompatible params:", aff, link)
