# Current Status

- [x] implementing `A Hierarchical DBSCAN Method for Extracting Microservices from Monolithic Applications`

- [x] implementing evaluation measures to test the the _Hierarchical DBSCAN Method_ on some _Test_Projects_

- [x] implementing `MICROscope` Method; using CodeBERT and fuzzy c-means clustering

- [x] implementing evaluation measures to test the the _MICROscope Method_ on some _Test_Projects_

- [ ] compare Hierarchical DBSCAN and MICROscope

- [ ] optimize & improve the _MICROscope Method_


# How to Use

First of all, make sure you have Java installed and the `java` command is available:

```
java -version
```

Then, install python requirements:

```
python -m pip install -r requirements.txt
```


## MICROscope

example usage:

```
python MICROscope.py --help
```

```
python MICROscope.py -p ./Test_Projects/JPetStore/src/ -e ICP NED SR -k 7 -o output.json
```

```
python MICROscope.py -f ./Test_Projects/JPetStore/OneFileSource.java -e IFN NED SM --alpha 0.5 --n-clusters 3 --threshold 0.4
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
