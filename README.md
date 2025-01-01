# Current Status

- [x] implement `A Hierarchical DBSCAN Method for Extracting Microservices from Monolithic Applications`

- [x] implement evaluation measures to test the the _Hierarchical DBSCAN Method_ on some _Test_Projects_

- [x] implement `Mono2Multi` Method; using CodeBERT and fuzzy c-means clustering

- [x] implement evaluation measures to test the the _Mono2Multi Method_ on some _Test_Projects_

- [x] compare Hierarchical DBSCAN and Mono2Multi

- [x] optimize & improve the _Mono2Multi Method_

- [x] implement `Mo2oM` Method; using CodeBERT and Neural Overlapping Community Detection

- [ ] conduct extensive experiments

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

This script facilitates experiments with the Mono2Multi method. You can utilize it to run the Mono2Multi method while varying any of the hyperparameters over a specified interval. 
For example, you can gradually increase the threshold from 0.1 to 0.7 and save the output of each run in the `output.json` file:

```
python Mono2Multi_expt.py -f Test_Projects/JPetStore/OneFileSource.java --alpha 1 --n-clusters 3 --threshold 0.1 0.7 -e ICP NED -o output.json
```


## Mo2oM

example usage:

```
python Mo2oM.py --help
```

```
python Mo2oM.py -p ./Test_Projects/JPetStore/src/ -e ICP NED SR -k 7 -o output.json
```

```
python Mo2oM.py -f ./Test_Projects/JPetStore/OneFileSource.java -e IFN NED SM --alpha 0.5 --n-clusters 3 --threshold 0.4
```


## Mo2oM_expt

This script facilitates experiments with the Mo2oM method. You can utilize it to run the Mo2oM method while varying any of the hyperparameters over a specified interval. 
For example, you can gradually increase the threshold from 0.1 to 0.7 and save the output of each run in the `output.json` file:

```
python Mo2oM_expt.py -f Test_Projects/JPetStore/OneFileSource.java --alpha 1 --n-clusters 3 --threshold 0.1 0.7 -e ICP NED -o output.json
```


## HDBSCAN

example usage:

```
python HDBSCAN.py --help
```

```
python HDBSCAN.py -p ./Test_Projects/JPetStore/src/ -e ICP NED SR -k 7 -o output.json
```

```
python HDBSCAN.py -f ./Test_Projects/JPetStore/OneFileSource.java -e IFN NED SM --alpha 0.5 --min-samples 2 --max-epsilon 0.75 -1
```


## HDBSCAN_expt

This script facilitates experiments with the HDBSCAN method. You can utilize it to run the HDBSCAN method while varying any of the hyperparameters over a specified interval. 
For example, you can gradually increase alpha from 0.45 to 0.55 and epsilon from 0.5 to 0.7 and save the output of each run in the `output.json` file:

```
python HDBSCAN_expt.py -f Test_Projects/JPetStore/OneFileSource.java --alpha 0.45 0.55 --epsilon 0.5 0.7 -e ICP NED -o output.json
```


## Acknowledgements

We would like to express our gratitude to the creators of the following repositories:

- [NOCD](https://github.com/shchur/overlapping-community-detection)
- [UniXcoder](https://github.com/microsoft/CodeBERT/tree/master/UniXcoder)
- [Mono2Micro-FSE-2021](https://github.com/kaliaanup/Mono2Micro-FSE-2021)

Their efforts and contributions to open-source development have been invaluable to this project.
