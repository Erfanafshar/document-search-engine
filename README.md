#   Information Retrieval Project

This project implements a multi-stage information retrieval system in Java. The goal was to build a functional search engine, progressing from basic retrieval to more advanced techniques.

##   Project Overview

The project was divided into three main phases, each building upon the previous one:

**Phase 1: Basic Information Retrieval**

* Implemented core components for indexing and retrieving documents.
* Tokenization of documents was performed to extract key terms.
* An inverted index was constructed to enable efficient document retrieval based on keywords.
* Text normalization techniques, including stemming, were applied to improve search accuracy. At least 5 normalization rules were implemented.
* Stop words were removed to filter out common, non-informative words.
* The system supports basic keyword queries to retrieve relevant documents.

**Phase 2: Vector Space Model and Ranking**

* Advanced the retrieval model to rank search results based on relevance.
* Documents were represented using the Vector Space Model, with TF-IDF weighting to capture term importance.
    * TF-IDF (Term Frequency-Inverse Document Frequency) was used to weight the importance of terms within documents.
* Cosine similarity was employed as a metric to measure the similarity between queries and documents.
* Efficiency was improved using techniques like index elimination and heap-based retrieval of top-K results.
* Champion lists were used to speed up query processing.

**Phase 3: Clustering for Enhanced Retrieval (Optional)**

* Explored the use of document clustering to improve search relevance and speed.
* Leveraged pre-existing category data from Wikipedia as clusters.
* Documents and cluster centroids were represented as vectors.
* Search queries were initially compared to cluster centroids to identify the most relevant cluster.
* Final document ranking was performed within the selected cluster.

##   Technology Stack

* Java was the primary programming language used for development.

##   Key Concepts

This project demonstrates several important information retrieval concepts:

* **Inverted Index:** A fundamental data structure for efficient keyword-based search.
* **TF-IDF:** A widely used technique for weighting term importance in documents.
* **Vector Space Model:** A mathematical model for representing documents and queries as vectors.
* **Cosine Similarity:** A measure of similarity between two non-zero vectors.
* **Document Clustering:** Grouping similar documents together to improve search efficiency and relevance.
* **Champion Lists:** An optimization technique for faster query processing.

##   Future Work

* Further optimization of search algorithms.
* Implementation of query expansion techniques.
* Development of a user interface for the search engine.
