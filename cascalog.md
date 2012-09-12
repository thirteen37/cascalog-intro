title: Introduction to Cascalog
author: Lim Yu-Xi

## Outline

* What is Hadoop and Cascalog?
* Why Cascalog?
* Basic queries
  * Where
  * Join
* More complex queries
  * Sub-queries
  * Aggregators
  * Custom operations

## What is Hadoop?

Distributed data-intensive processing. Not just a database.

Open source Apache project.

Mimics two main Google projects: Google MapReduce (computation) and Google File System (storage).

Useful for batch processing of large amounts of data, e.g., log files, analytics, data mining.

Very popular and widely used.

<http://en.wikipedia.org/wiki/Apache_Hadoop>

## What is Cascalog?

[Cascalog.org](http://www.cascalog.org/)

Domain-specific language (DSL) for writing Hadoop jobs in Clojure.

High-level declarative syntax similar to SQL. Don't worry about mappers and reducers.

Built on [Cascading](http://www.cascading.org/). Alternative to [Pig](http://pig.apache.org/)
and [Hive](http://hive.apache.org/).

## Why Cascalog?

(vs Pig, Hive, etc)

Integrates with your favourite language.

Additional integration with Java via [JCascalog](https://github.com/nathanmarz/cascalog/wiki/JCascalog).

Great test framework via [Midje Cascalog](https://github.com/sritchie/midje-cascalog).

Higher level abstraction than [clojure-hadoop](https://github.com/stuartsierra/clojure-hadoop).

## Getting Started

1. Use leiningen to create a project

       lein new cascalog-intro

2. Add cascalog to the project `:dependencies` and Hadoop to `:dev-dependencies`

       :dependencies [[cascalog "1.10.0"]]
       :dev-dependencies [[org.apache.hadoop/hadoop-core "0.20.2-dev"]]

3. We'll use the REPL for now

       lein repl

4. Use Cascalog's sample data

       (use 'cascalog.playground)
       (bootstrap)

## A First Query

`?<-` is Cascalog for specifying and running a query.

	(?<- (stdout)
	     [?person]
	     (age :> ?person ?age))

Output (`(stdout)`) is a tap. Taps are Cascading's abstraction of an input or output.
`(stdout)` creates a tap that prints to stdout.

`[?person]` is a vector of output fields. We can also do `[?person ?age]` to get each person's age.

Input (`age`) is a generator.

`(age ?person ?age)` is a predicate, using a generator to output (`:>`) to two variables.
`:>` is redundant in this case.

## Where Clauses

Search for people with age = 25 by replacing `?age` with a constant.

	(?<- (stdout)
	     [?person]
	     (age ?person 25))

Search for people with age < 30 using an operator.

	(?<- (stdout)
	     [?person]
	     (age ?person ?age)
	     (< :< ?age 30))

Again, the `:<` is redundant. Clojure functions can be used as operators.

## Joins

A more complex query

    (?<- (stdout)
	     [?person]
         (follows "emily" ?person)
         (gender ?person "m"))

What does this do?

Cascalog is declarative, so order doesn't matter.

    (?<- (stdout)
	     [?person1 ?person2] 
         (age ?person1 ?age1)
         (follows ?person1 ?person2)
         (age ?person2 ?age2)
         (< ?age2 ?age1))

## Sorting and Duplicates

Default sort by first field. Can sort by additional fields using `:sort` and `:reverse`.

	(defbufferop first-tuple [tuples] (take 1 tuples))
    (?<- (stdout)
         [?person ?youngest]
         (follows ?person ?p2)
         (age ?p2 ?age)
         (:sort ?age)
         (:reverse true)
         (first-tuple ?p2 :> ?youngest))

Remove duplicates by adding `(:distinct true)`.

    (?<- (stdout) [?a] (age _ ?a))
    (?<- (stdout) [?a] (age _ ?a) (:distinct true))

## Generators

Generators so far are actually Clojure sequences.

Generators can also be other queries (covered later), or Cascading taps, which in turn can be files,
databases, message queues, etc.

Cascalog provides a few built-in taps: `lfs-seqfile` and `lfs-textline` for **local** file systems,
and `hfs-seqfile` and `hfs-textline` for **HDFS**.

Create a generator from a text file:

    (def text-generator (lfs-textline "cascalog.md"))

Read and display lines from a text file:

    (?<- (stdout)
         [?line]
         (text-generator ?line))

## Sub-Queries

Define a query using `<-`. Execute a query using `?-`.

Queries are composable:

    (let [many-follows (<- [?person]
                           (follows ?person _)
                           (c/count ?c)
                           (> ?c 2))]
      (?<- (stdout)
           [?person1 ?person2]
           (many-follows ?person1)
           (many-follows ?person2)
           (follows ?person1 ?person2)))

Query planner smart enough not to perform `many-follows` twice.

## Aggregators

Cascalog aggregators are similar to SQL aggregators (`GROUP BY`).
There are a few built in and you can define your own.

Find the number of people who are less than 30 years old:

    (?<- (stdout)
	     [?count]
         (age _ ?a)
         (< ?a 30)
         (c/count ?count))

`_` is standard Clojure syntax for ignoring a variable.

We can also do `GROUP BY` by using partitions.

Partition by person being followed, i.e., how many people follow a person.

    (?<- (stdout)
	     [?person ?count]
	     (follows ?person _)
         (c/count ?count))

## Multiple Aggregators

    (?<- (stdout)
	     [?country ?avg] 
         (location ?person ?country _ _)
	     (age ?person ?age)
         (c/count ?count)
	     (c/sum ?age :> ?sum)
         (div ?sum ?count :> ?avg))

## Custom Operators

Any Clojure function can be used as an operator. 

More advanced operators: mappers, filters, and aggregators. Have to think about Hadoop's mappers and reducers.

Split lines into words:

    (defmapcatop split [sentence]
      (seq (.split sentence "\\s+")))

Looks very much like a regular `defn`.

Count words in a text file:

    (?<- (stdout)
         [?count]
         (text-generator ?line)
         (split ?line :> ?word)
	     (c/count ?count))

Many more ways of defining custom operators:
`defmapop`, `deffilterop`, `defbufferop`, `defaggregateop`, `defparallelagg`.

## What's next

Only just getting started. There's more...

* Outer joins
* Other query operators
* Other predicate operators
* Predicate operators
* Midje Cascalog
* Cascalog contrib and other utilities
* Deploying on Hadoop and EMR

## Further reading

Most examples are from:

* [Introducing Cascalog: a Clojure-based query language for Hadoop](http://nathanmarz.com/blog/introducing-cascalog-a-clojure-based-query-language-for-hado.html)
* [New Cascalog features: outer joins, combiners, sorting, and more](http://nathanmarz.com/blog/new-cascalog-features-outer-joins-combiners-sorting-and-more.html)

Additional information:

* [Official Wiki](https://github.com/nathanmarz/cascalog/wiki)
* [News Feed in 38 lines of code using Cascalog](http://nathanmarz.com/blog/news-feed-in-38-lines-of-code-using-cascalog.html)
* Nathan Marz's [Big Data](http://www.manning.com/marz/)

## TravelShark

<http://www.travelshark.com>

[TravelShark Dive](http://www.travelshark.com/dive/)

We're hiring backend developers!

* Natural language processing, Machine learning
* Hadoop, Clojure, Cascalog

