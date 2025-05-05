import matplotlib.pyplot as plt
import networkx as nx
import seaborn as sns
import numpy as np


MODEL = "Mo2oM"
PROJECT = "JPetStore"
MICROSERVICES = [   # you can get these from ../top_microservices/
	['Account', 'Product', 'AccountActionForm', 'SqlMapCategoryDao', 'OrderActionForm', 'AccountFormController', 'BaseActionForm', 'AccountForm'],
	['Account', 'Item', 'SqlMapItemDao', 'AddItemToCartAction', 'NewOrderFormAction', 'SecureBaseAction', 'ListOrdersController', 'RemoveItemFromCartAction', 'UpdateCartQuantitiesController', 'RemoveItemFromCartController', 'MsSqlOrderDao', 'Cart', 'CartActionForm', 'SendOrderConfirmationEmailAdvice', 'LineItem', 'UpdateCartQuantitiesAction', 'ViewCartController', 'OrderServiceClient'],
	['Account', 'SqlMapItemDao', 'PetStoreImpl', 'CartActionForm', 'UpdateCartQuantitiesAction'],
	['Account', 'OrderForm', 'Order', 'OrderFormController', 'OrderActionForm', 'OrderValidator', 'MsSqlOrderDao', 'Category', 'NewOrderAction', 'ViewOrderController', 'SqlMapProductDao', 'SendOrderConfirmationEmailAdvice', 'LineItem', 'SignonInterceptor', 'AccountForm', 'SqlMapOrderDao', 'OrderServiceClient'],
	['Account', 'NewOrderAction'],
	['Account', 'OrderService'],
	['Account', 'Product', 'AccountActionForm', 'SqlMapItemDao', 'SecureBaseAction', 'PetStoreImpl', 'ViewProductAction'],
	['Account', 'ViewProductController', 'AddItemToCartController', 'AddItemToCartAction', 'SearchProductsAction', 'ProductSearch', 'ViewCategoryController', 'ViewItemController', 'MsSqlOrderDao', 'ViewItemAction', 'NewOrderAction', 'SearchProductsController', 'BaseAction', 'PetStoreFacade', 'ViewCategoryAction', 'ViewProductAction'],
	['Account', 'ViewCategoryController', 'PetStoreImpl'],
	['AddItemToCartController', 'Item', 'AddItemToCartAction', 'NewOrderFormAction', 'UserSession', 'RemoveItemFromCartAction', 'UpdateCartQuantitiesController', 'RemoveItemFromCartController', 'NewOrderAction', 'CartItem', 'Cart', 'CartActionForm', 'PetStoreFacade', 'UpdateCartQuantitiesAction', 'ViewCartController'],
	['Account', 'ViewProductController', 'AccountActionForm', 'NewOrderFormAction', 'UserSession', 'SecureBaseAction', 'ListOrdersController', 'SearchProductsAction', 'Order', 'AccountDao', 'SignonAction', 'ListOrdersAction', 'EditAccountAction', 'ViewOrderController', 'SignonController', 'Cart', 'CartActionForm', 'AccountFormController', 'ViewCartAction', 'SearchProductsController', 'SendOrderConfirmationEmailAdvice', 'NewAccountFormAction', 'NewAccountAction', 'BaseAction', 'EditAccountFormAction', 'PetStoreFacade', 'ViewCategoryAction', 'AccountForm', 'ViewProductAction', 'SqlMapOrderDao', 'ViewOrderAction', 'SqlMapAccountDao'],
	['Account', 'AccountActionForm', 'SecureBaseAction', 'ListOrdersController', 'ProductSearch', 'OrderFormController', 'ListOrdersAction', 'BaseActionForm'],
	['Account', 'AccountActionForm', 'ViewCategoryController', 'OrderActionForm', 'OrderValidator', 'PetStoreImpl', 'AccountValidator', 'SqlMapSequenceDao'],
	['Account', 'AccountActionForm', 'SqlMapItemDao', 'OrderFormController', 'OrderValidator', 'PetStoreImpl', 'NewAccountFormAction', 'SqlMapAccountDao'],
	['Account', 'CategoryDao', 'SqlMapItemDao', 'PetStoreImpl'],
	['Account', 'SqlMapItemDao', 'UserSession', 'AccountDao', 'PetStoreImpl', 'OrderService'],
	['Account', 'CategoryDao'],
	['Item', 'SecureBaseAction', 'ListOrdersController', 'Order', 'OrderFormController', 'OrderActionForm', 'OrderValidator', 'ListOrdersAction', 'MsSqlOrderDao', 'Category', 'AccountValidator', 'NewOrderAction', 'CartItem', 'ViewOrderController', 'SendOrderConfirmationEmailAdvice', 'SqlMapSequenceDao', 'SqlMapOrderDao', 'OrderServiceClient', 'SqlMapAccountDao'],
	['Account', 'OracleSequenceDao'],
	['Account', 'OrderValidator', 'SqlMapProductDao'],
	['Account', 'Item', 'SqlMapItemDao', 'NewOrderFormAction', 'ListOrdersController', 'Order', 'UpdateCartQuantitiesController', 'ListOrdersAction', 'MsSqlOrderDao', 'Category', 'NewOrderAction', 'CartItem', 'ViewOrderController', 'SendOrderConfirmationEmailAdvice', 'SqlMapSequenceDao', 'LineItem', 'SqlMapOrderDao'],
]

communities = []
count = 0
for ms in MICROSERVICES:
    community = list(range(count, count + len(ms)))
    communities.append(community)
    count += len(ms)

node_colors = []
for i, ms in enumerate(MICROSERVICES):
    node_colors.extend([sns.color_palette("Set2", n_colors=len(communities))[i]] * len(ms))

G = nx.Graph()
G.add_nodes_from(range(count))


def generate_non_overlapping_positions(nodes, min_distance, area_size):
    positions = {}
    for node in nodes:
        while True:
            x, y = np.random.uniform(0, area_size, 2)
            pos = np.array([x, y])
            too_close = False
            for existing_pos in positions.values():
                distance = np.linalg.norm(pos - existing_pos)
                if distance < min_distance:
                    too_close = True
                    break
            if not too_close:
                positions[node] = pos
                break
    return positions


def scale_and_center_positions(pos, center, scale=1):
    positions = np.array(list(pos.values()))
    centroid = positions.mean(axis=0)
    new_positions = {}
    for node, (x, y) in pos.items():
        new_x = (x - centroid[0]) * scale + center[0]
        new_y = (y - centroid[1]) * scale + center[1]
        new_positions[node] = (new_x, new_y)
    return new_positions


supergraph = nx.Graph()
n = len(MICROSERVICES)
supergraph.add_nodes_from(range(n))
superpos = generate_non_overlapping_positions(range(n), min_distance=1.7, area_size=8.5)
centers = list(superpos.values())
pos = {}
for center, comm in zip(centers, communities):
    print(".", end="", flush=True)
    pos.update(scale_and_center_positions(generate_non_overlapping_positions(nx.subgraph(G, comm).nodes, 0.225, 1.4), center=center))
print()

nx.draw_networkx_nodes(G, pos, node_color=node_colors, alpha=1, node_size=15)
plt.axis('off')
plt.savefig(f"{MODEL}_{PROJECT}_Dist_Graph.pdf", bbox_inches='tight')
plt.show()
