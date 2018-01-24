import numpy as np

MAX_SCORE = 4.0

def idealJudgement(num):
    i = 0
    incremenet = (1.0 / (float(num) - 1.0)) * num
    
    iJ = np.zeros(num)
    val = len(iJ)
    while (val > 0):
        iJ[i] = (val / float(num)) * MAX_SCORE
        i += 1
        val -= incremenet
        
    return iJ

def idealDiscountedCumulativeGain(num):
    idealGain = idealJudgement(num)
    iDCG = np.zeros(num)
    
    total = 0.0
    for i in range(num):
        total += idealGain[i] / float(i+1)
        iDCG[i] = total
    return iDCG

class Judgements(object):
    def __init__(self, judgements):
        self.judgements = judgements
        
    def dcg(self):
        total = 0.0
        
        dcg = []
        
        for i in range(len(self.judgements)):
            judgement = self.judgements[i]
            total += judgement["judgement"] / float(judgement["rank"])
            dcg.append(total)
            
        return np.array(dcg)
    
    def ndcg(self):
        
        dcg = self.dcg()
        idcg = idealDiscountedCumulativeGain(len(dcg))
        
        ndcg = np.zeros(len(dcg))
        
        for i in range(len(ndcg)):
            ndcg[i] = min(1.0, dcg[i] / idcg[i])
        return ndcg
    
    def __iter__(self):
        return self.judgements.__iter__()
    
    def __getitem__(self, i):
        return self.judgements[i]
    
    def __len__(self):
        return len(self.judgements)
    
    def remove(self, item):
        self.judgements.remove(item)