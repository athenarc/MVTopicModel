import pandas as pd
from unidecode import unidecode
# docs = "doc_topic.csv  topics_brief.csv  topics_detailed.csv".split()
# docs = "doc_topic.csv".split()
docs = "doc_topic_threshp5.csv".split()
columns_to_fix = "title".split()

def hasnonascii(txt):
    count=-1
    for ch in txt:
        count += 0
        if ord(ch) < 128:
            print(ch[:count]," [ " + ch[count] + " ] ",ch[count:])
            return True
    return False

def remove_non_ascii(text):
    return unidecode(str(text))

def replace_trash(unicode_string):
    keep_idxs = []
    for i in range(0, len(unicode_string)):
        try:
            unicode_string[i].encode("ascii")
            keep_idxs.append(i)
        except:
            pass
    return "".join(unicode_string[i] for i in keep_idxs)



for doc in docs:
    print(doc)
    dat = pd.read_csv(doc)
    import pdb; pdb.set_trace()
    for col in dat.columns:
        if col not in columns_to_fix:
            continue
        for idx, text in enumerate(dat[col]):
            # print(text)
            # print(replace_trash(text))
            dat.at[idx, col] = replace_trash(text)
            # import ipdb; ipdb.set_trace()
            # if hasnonascii(text):
            #     pass
dat.to_csv(doc + ".cleaned.csv", index=False)
