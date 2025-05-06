from Mo2oM import nocd
import scipy.sparse as sp
import numpy as np
import torch
import torch.nn.functional as F

torch.manual_seed(42)
torch.cuda.manual_seed(42)
torch.cuda.manual_seed_all(42)
torch.backends.cudnn.deterministic = True
torch.use_deterministic_algorithms(True)
np.random.seed(42)

def overlapping_community_detection(A, X, K, threshold):
    # set torch device: cuda vs cpu
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    torch.set_default_device(device)
    print(f"[NOCD] using device {device}", flush=True)

    x_norm = sp.hstack([X, A])
    x_norm = nocd.utils.to_sparse_tensor(x_norm).to(device)

    # hyperparameters
    hidden_sizes = [128]    # hidden sizes of the GNN
    weight_decay = 1e-2     # strength of L2 regularization on GNN weights
    dropout = 0.5           # whether to use dropout
    batch_norm = True       # whether to use batch norm
    lr = 1e-3               # learning rate
    max_epochs = 500        # number of epochs to train
    balance_loss = True     # whether to use balanced loss
    stochastic_loss = True  # whether to use stochastic or full-batch training
    batch_size = 20000      # batch size (only for stochastic training)

    sampler = nocd.sampler.get_edge_sampler(A, batch_size, batch_size, num_workers=2)
    gnn = nocd.nn.GCN(x_norm.shape[1], hidden_sizes, K, dropout=dropout, batch_norm=batch_norm).to(device)
    adj_norm = gnn.normalize_adj(A)
    decoder = nocd.nn.BerpoDecoder(A.shape[0], A.nnz, balance_loss=balance_loss)
    opt = torch.optim.Adam(gnn.parameters(), lr=lr)

    val_loss = np.inf
    validation_fn = lambda: val_loss
    early_stopping = nocd.train.NoImprovementStopping(validation_fn, patience=10)
    model_saver = nocd.train.ModelSaver(gnn)

    for epoch, batch in enumerate(sampler):
        if epoch > max_epochs:
            break
        if epoch % 25 == 0:
            with torch.no_grad():
                gnn.eval()
                # Compute validation loss
                Z = F.relu(gnn(x_norm, adj_norm))
                val_loss = decoder.loss_full(Z, A)
                print(f'[NOCD] Epoch {epoch:4d}, loss.full = {val_loss:.4f}')
                # Check if it's time for early stopping / to save the model
                early_stopping.next_step()
                if early_stopping.should_save():
                    model_saver.save()
                if early_stopping.should_stop():
                    print(f'[NOCD] Breaking due to early stopping at epoch {epoch}')
                    break
                
        # Training step
        gnn.train()
        opt.zero_grad()
        Z = F.relu(gnn(x_norm, adj_norm))
        ones_idx, zeros_idx = batch
        if stochastic_loss:
            loss = decoder.loss_batch(Z, ones_idx, zeros_idx)
        else:
            loss = decoder.loss_full(Z, A)
        loss += nocd.utils.l2_reg_loss(gnn, scale=weight_decay)
        loss.backward()
        opt.step()

    Z = F.relu(gnn(x_norm, adj_norm))
    memberships = Z.cpu().detach().numpy()

    if threshold == None:
        import matplotlib.pyplot as plt
        plt.hist(Z[Z > 0].cpu().detach().numpy(), 100);
        plt.show()
        threshold = float(input("enter degree of membership threshold: "))
    elif isinstance(threshold, str):
        if threshold == "max":
            print("[Mo2oM] using hard clustering")
            return [{int(ms)} for ms in np.argmax(memberships, axis=1)]
        else:
            print("[Mo2oM] invalid value for threshold")
            return
    elif not isinstance(threshold, (int, float)):
        layers = []
        for threshold_i in threshold:
            classes_microservices = np.full(memberships.shape, -1)
            print(f"[Mo2oM] threshold = {threshold_i}")
            for col in range(memberships.shape[1]):
                classes_microservices[:,col][memberships[:,col]>=threshold_i] = col
            classes_microservices = classes_microservices.astype(int)
            clusters = []
            for row in classes_microservices:
                row = set(row)
                if len(row) > 1:
                    row.discard(-1)
                clusters.append(row)
            layers.append(clusters)
        return layers

    for col in range(memberships.shape[1]):
        memberships[:,col][memberships[:,col]<threshold] = -1
        memberships[:,col][memberships[:,col]>=threshold] = col
    memberships = memberships.astype(int)
    clusters = []
    for row in memberships:
        row = set(row)
        if len(row) > 1:
            row.discard(-1)
        clusters.append(row)

    return clusters
