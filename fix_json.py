from sys import argv


file_name = argv[1]
in_ms = False
inn_ms = False

with (open(file_name, "r+") as in_f,
      open(file_name.replace("Out", ""), "wt") as out_f):
    for line in in_f.readlines():
        if line.endswith("[\n"):
            if in_ms:
                inn_ms = True
            elif "microservices" in line:
                in_ms = True
                out_f.write(line[:-1])
                continue
        elif line.endswith("],\n") or line.endswith("]\n"):
            if inn_ms:
                inn_ms = False
            else:
                in_ms = False
                out_f.write(line.lstrip())
                continue
        if in_ms:
            out_f.write(line.strip())
        else:
            out_f.write(line)
