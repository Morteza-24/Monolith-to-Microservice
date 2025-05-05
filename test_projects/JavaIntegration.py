# You can use this file to integrate java files into one java file
import os
import shutil


def integrate_java_files(src_dir, dest_file):
    with open(dest_file, 'w') as outfile:
        for root, dirs, files in os.walk(src_dir):
            for filename in files:
                if filename.endswith('.java'):
                    filepath = os.path.join(root, filename)
                    with open(filepath) as infile:
                        lines = infile.readlines()
                        for line in lines:
                            if not line.startswith('package'):
                                outfile.write(line)
                    outfile.write('\n')



integrate_java_files(input("src dir: "), input("dest file: "))
