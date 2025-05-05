from sklearn.linear_model import LinearRegression
from numpy import array


clusters = array(range(2,10)).reshape(-1,1)

petclinic = [0.45, 0.5, 0.4, 0.3, 0.3, 0.15, 0.15, 0.15]
jpetstore = [0.6, 0.4, 0.3, 0.25, 0.25, 0.2, 0.15, 0.15]
daytrader = [0.5, 0.5, 0.4, 0.3, 0.2, 0.2, 0.2, 0.15]
acmeair = [0.45, 0.4, 0.3, 0.25, 0.2, 0.2, 0.15, 0.15]
# plants = [0.65, 0.4, 0.25, 0.2, 0.2, 0.15, 0.15, 15]

coefs = []
intercepts = []

for y in petclinic, jpetstore, daytrader, acmeair:
    reg = LinearRegression().fit(clusters, y)
    coefs.append(reg.coef_[0])
    intercepts.append(reg.intercept_)
    print("score:", reg.score(clusters, y))
    print("coef:", reg.coef_[0])
    print("coef", reg.intercept_)
    print()

coef = sum(coefs)/len(coefs)
intercept = sum(intercepts)/len(intercepts)

print(f"avg_coef: {coef}\navg_intercept: {intercept}\n")

for x in range(2,10):
    y = coef*x+intercept
    print(f"{x}: {round(y,2)}")
