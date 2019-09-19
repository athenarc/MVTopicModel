import json
import sys

import numpy as np
from matplotlib import pyplot as plt

input_json = sys.argv[1]
categ = sys.argv[2] if len(sys.argv) > 2 else None

# read topic similarity responses
print("Reading json")
with open(input_json) as f:
    data = json.load(f)

# process json into a similarity matrix 
topicids = set([d['topicId1'] for d in data] + [d['topicId2'] for d in data])

num_items = len(topicids)
sims = np.random.random((num_items, num_items))
sorted_ids = sorted(topicids)

for dat in data:
    t1, t2, sim = dat['topicId1'], dat['topicId2'], dat['similarity']
    idx1, idx2 = sorted_ids.index(t1), sorted_ids.index(t2)
    sims[idx1][idx2] = sim


num_categories = 5
threshold = 0.8
np.random.seed(1234)
boolean_sims = np.array(sims>threshold).astype(np.int32)

# assign categories
categories = list(range(num_categories))
assig = [np.random.choice(categories) for _ in sims]
print("Assignments:", assig)

category_sims = np.zeros((num_categories, num_categories), np.int32)

for cat in categories:
    curr_cat_rows = np.asarray([i for i in range(len(assig)) if assig[i] == cat])
    # print("category", cat, curr_cat_rows)
    # get comparison with other categories
    # for other_cat in [x for x in categories if x != cat]:
    for other_cat in categories:
        # get items in the other category
        other_cat_cols = np.asarray([i for i in range(len(assig)) if assig[i] == other_cat])
        # print("\t", other_cat_cols)
        # sum up the similarity of elements in this category with the other category
        category_sims[cat, other_cat] = np.sum(boolean_sims[curr_cat_rows][:,other_cat_cols])

print(category_sims)
# write to json
with open("category_similarities_num_{}.json".format(num_categories), "w") as f:
    json.dump(category_sims.tolist(), f)
