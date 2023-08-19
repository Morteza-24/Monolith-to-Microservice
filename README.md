# Current Status

- [x] implementing `A Hierarchical DBSCAN Method for Extracting Microservices from Monolithic Applications`

- [x] implementing evaluation measures to test the the _Hierarchical DBSCAN Method_ on some _Test_Projects_


# How to Use

1. Make sure you have Java installed and the `java` command is available.
2. Make sure that `numpy`, `nltk` and `scikit-learn` are installed:
```
python -m pip install numpy nltk scikit-learn
```
3.
```
$ python main.py --help
```

```
usage: python main.py [-h] [-f FILE_PATH] [-p PROJECT_DIRECTORY] [-e [{Precision,SR,SM,IFN,NED,ICP} ...]] [-k [K ...]]

This program offers tools related to migrating from monolithic architectures to microservices.

options:
  -h, --help            show this help message and exit
  -f FILE_PATH, --file FILE_PATH
                        path to the java source code file (use this option if your whole monolithic program is in one file)
  -p PROJECT_DIRECTORY, --project PROJECT_DIRECTORY
                        path to the java project directory, (use this option if your monolithic program is in multiple files) this option overrides --file
  -e [{Precision,SR,SM,IFN,NED,ICP} ...], --evaluation-measure [{Precision,SR,SM,IFN,NED,ICP} ...]
                        For the Precision and the SuccessRate (SR) measures, the ground truth microservices must be in different directories of your project's root directory. And for the SR measure you should also use the -k option to specify a threshold.
  -k [K ...]            The k value for the SR measure (e.g. SR@7). This option can only be used if you are using the SR measure.

example usage: python main.py -p ./Test_Projects/PetClinic/src -e NED ICP SR -k 5 7
```
