# Current Status

- [x] implementing `A Hierarchical DBSCAN Method for Extracting Microservices from Monolithic Applications`

- [x] implementing evaluation measures to test the the _Hierarchical DBSCAN Method_ on some _Test_Projects_

- [x] implementing `Mono2Multi` Method; using CodeBERT and fuzzy c-means clustering

- [x] implementing evaluation measures to test the the _Mono2Multi Method_ on some _Test_Projects_

- [ ] compare Hierarchical DBSCAN and Mono2Multi

- [ ] optimize & improve the _Mono2Multi Method_


# How to Use

First of all, make sure you have Java installed and the `java` command is available:

```
java -version
```

Then, install python requirements:

```
python -m pip install -r requirements.txt
```


## Mono2Multi

example usage:

```
python Mono2Multi.py --help
```

```
python Mono2Multi.py -p ./Test_Projects/JPetStore/src/ -e ICP NED SR -k 7 -o output.json
```

```
python Mono2Multi.py -f ./Test_Projects/JPetStore/OneFileSource.java -e IFN NED SM --alpha 0.5 --n-clusters 3 --threshold 0.4
```


## Mono2Multi_expt

example usage:
```
python Mono2Multi_expt.py -f Test_Projects/JPetStore/OneFileSource.java --alpha 1 --n-clusters 3 --threshold 0.1 0.7 --n-execs 5 -e ICP NED -o output.json
```


## Hierarchical_DBSCAN

example usage:

```
python hierarchical_DBSCAN.py --help
```

```
python hierarchical_DBSCAN.py -p ./Test_Projects/JPetStore/src/ -e ICP NED SR -k 7 -o output.json
```

```
python hierarchical_DBSCAN.py -f ./Test_Projects/JPetStore/OneFileSource.java -e IFN NED SM --alpha 0.5 --min-samples 2 --max-epsilon 0.75 -1
```
