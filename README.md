# MVTopicModel: Probabilistic Multi View Topic Modelling engine
Omiros Metaxas

### General Notes
Non-parametric Multi-View Topic Model (MViHDP) that extends well-established Hierarchical Dirichlet Process (HDP) 
incorporating a novel Interacting Pólya Urn scheme (IUM) to model per-document topic distribution. 
This way, MViHDP combines interaction and reinforcement addressing the following major challenges: 
1) multi-view modeling leveraging statistical strength among different views, 
2) estimation and adaptation to the extent of correlation between the different views that is automatically inferred during inference and 
3) scalable inference on massive, real world datasets. The latter is achieved through a parallel Gibbs sampling scheme that utilizes efficient F+Tree data structure. 

We consider that estimating the right number of topics is not our primary goal especially in collections of that size. 
So, we have implemented a truncated version of the proposed model where the goal is to better estimate priors and model parameters given a maximum number of topics. 

Although initially we extended MALLET’s efficient parallel topic model with Sparse LDA sampling, we end up implementing a very different parallel implementation based on F+Trees 
that: 
- it is more readable and extensible, 
- it is usually faster in real world big datasets especially in multi-view settings 
- shares model related data structures across threads (contrary to MALLET where each thread retains each own model copy) 
We update the model using background threads based on a lock free, queue based scheme minimizing staleness. 
Our implementation can scale to massive datasets (over one million documents with meta-data and links) on a single computer.

Related classes in package cc.mallet.topics:
- FastQMVWVParallelTopicModel: Main class
- FastQMVWVUpdaterRunnable: Model updating
- FastQMVWVWorkerRunnable: Gibbs Sampling
- FastQMVWVTopicModelDiagnostics: Related diagnostics 
- PTMFlow, PTMExperiment: Example of how we can load data from postrgres or SQLite DBs, run MV-Topic Models, calc similarities etc

Example results (multi view topics on Full ACM & OpenAccess PubMed corpora):

https://1drv.ms/f/s!Aul4avjcWIHpg-Ara0PZzqHeOkyGIw


## Semantic annotator
### Installation
Follow installation instructions for a local server from [here](https://github.com/dbpedia-spotlight/dbpedia-spotlight-model#run-your-own-server), i.e.:
- Download the dbpedia-spotlight jar from [here](https://sourceforge.net/projects/dbpedia-spotlight/files/spotlight/dbpedia-spotlight-1.0.0.jar/download).
- Download a language model file from [here](https://sourceforge.net/projects/dbpedia-spotlight/files/2016-10/) and extract.
- Run locally via `java -jar dbpedia-spotlight-1.0.jar en http://localhost:2222/rest`


