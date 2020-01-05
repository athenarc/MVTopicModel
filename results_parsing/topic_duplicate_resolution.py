import pandas as pd
import numpy as np
import random
from collections import Counter

# if a previously duplicate source advances to a top-K level
# which is not already assigned and has no duplicates, immediately assign it
random.seed(1137)
outfile = "filtered_223_resolved_assignments_oct2june.csv"
sorted_similarities_path="/home/nik/athena/topics_socialbuzz_table/fix_curation_mapping/from_oct_to_june_closest_mapping_top_469_curation_considered.csv"
social_or_icd11_oct_topics = [ 0, 1, 2, 3, 4, 5, 6, 7, 8, 11, 12, 13, 14, 15, 17, 18, 24, 26, 27, 29, 30, 34, 35, 39, 43, 45, 47, 48, 53, 54, 55, 56, 59, 64, 65, 75, 77, 84, 91, 94, 95, 96, 97, 98, 100, 101, 102, 104, 106, 108, 109, 112, 115, 116, 117, 118, 120, 121, 123, 124, 125, 128, 132, 133, 135, 136, 140, 141, 143, 146, 152, 153, 156, 157, 158, 164, 165, 169, 171, 172, 173, 175, 178, 180, 184, 186, 187, 189, 190, 198, 204, 207, 209, 210, 212, 216, 220, 222, 228, 229, 231, 233, 240, 244, 245, 246, 247, 248, 250, 251, 254, 255, 258, 259, 260, 262, 263, 266, 271, 272, 274, 276, 279, 284, 285, 290, 291, 294, 295, 296, 298, 302, 303, 304, 305, 306, 308, 309, 312, 314, 315, 317, 318, 319, 320, 321, 323, 324, 326, 330, 332, 333, 335, 338, 341, 343, 344, 346, 347, 348, 349, 350, 351, 352, 353, 354, 355, 356, 359, 362, 366, 368, 369, 370, 373, 377, 378, 380, 385, 389, 397, 398, 400, 401, 402, 405, 406, 407, 419, 420, 423, 424, 425, 426, 431, 433, 434, 437, 440, 442, 444, 445, 446, 447, 449, 450, 454, 462, 465, 466, 469, 470, 471, 473, 475, 480, 482, 486, 487, 488, 491, 493, 499]
# source, target identifiers
header = "oct18", "june19"
assert len(social_or_icd11_oct_topics) == 223, "Fix filtering topic list, dumbass."

# RUN
def get_duplicates(assignment_vector):
    if len(assignment_vector) != len(set(assignment_vector)):
        cn = Counter(assignment_vector)
        duplicated_targets =  [x[0] for x in cn.most_common() if x[1] > 1]
        sources = []
        for dt in duplicated_targets:
            src = [i for i in range(len(assignment_vector)) if assignment_vector[i] == dt]
            sources.append(src)
        return sources, duplicated_targets
    return None, None




data = pd.read_csv(sorted_similarities_path)
# filter out icd / social
data = data.loc[social_or_icd11_oct_topics]
# TEST
# data = data.loc[data.index.tolist()[:50]]
print("Data shape", data.shape, "columns", data.columns[:5])

actual = pd.DataFrame(data = np.hstack((data.values[:,:3], np.zeros((len(data), 2),np.int32))), columns=["oct", "top", "sim", "losers", "level"])
actual = actual.set_index(data.index)
# actual = data.copy()[["oct", "june_top_1", "similarity_top_1"]]
# actual = actual.rename(columns={"june_top_1":"top", "similarity_top_1":"sim"})
assert all(actual.index == data.index) and len(actual) == len(data) and actual.shape == (len(data), 5), "Actual / data mismatch"
actual.loc[:, "losers"] = 0
actual.loc[:, "level"] =1

counter = 0
while True:
    print("Actual before the pass")
    print(actual.sort_values("top"))
    counter += 1
    # get all duplicate target topics and their source topics
    source_indexes, targets = get_duplicates(list(actual["top"].values))
    if targets is None:
        break
    print("Pass #", counter, "duplicate targets count:", len(targets))
    sources = [[data.index.tolist()[i] for i in srclist] for srclist in source_indexes]
    losers = []
    # accumulate all losers for this pass
    for src, targ in zip(sources, targets):
        assert all(actual.loc[src]["top"] == targ), "Whops."
        sims = actual.loc[src, "sim"] .tolist()
        maxsim = max(sims)
        print("Duplicate target:", targ, ": sources & similarities:", list(zip(src, sims)), "winner", src[sims.index(maxsim)])
        local_losers = [src[i] for i in range(len(src)) if i != sims.index(maxsim)]
        losers.extend(local_losers)
        actual.loc[local_losers, "losers"] = 1
    # edit losers for this pass
    assert sorted(losers) == sorted(actual[actual["losers"] == 1].index.tolist()), "Loser mismatch."
    print("Losers:", losers)
    # print("Actual after loser marks")
    # print(actual.sort_values("top"))
    for l in losers:
        # increase level
        level = int(actual.loc[l, "level"] + 1)
        # get next best match and similarity
        actual.loc[l, "level"] = level
        match = data.loc[l, "june_top_{}".format(level)]
        sim = data.loc[l, "similarity_top_{}".format(level)]
        actual.loc[l, "top"], actual.loc[l, "sim"] = match, sim
        print("Loser", l, "new level", level, "new match / sim", match, sim)
    actual.loc[losers, "losers"] = 0
        
    print("Actual after the pass")
    print(actual.sort_values("top"))
print("Done.")

actual[["oct", "top", "sim"]].to_csv("resolved_ioanna.csv", index=None)