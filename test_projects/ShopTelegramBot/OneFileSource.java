
import java.util.ArrayList;
import java.util.List;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import ua.ivanzaitsev.bot.core.ConfigReader;
import ua.ivanzaitsev.bot.core.TelegramBot;
import ua.ivanzaitsev.bot.handlers.ActionHandler;
import ua.ivanzaitsev.bot.handlers.CommandHandler;
import ua.ivanzaitsev.bot.handlers.UpdateHandler;
import ua.ivanzaitsev.bot.handlers.commands.CartCommandHandler;
import ua.ivanzaitsev.bot.handlers.commands.CatalogCommandHandler;
import ua.ivanzaitsev.bot.handlers.commands.OrderConfirmCommandHandler;
import ua.ivanzaitsev.bot.handlers.commands.OrderEnterAddressCommandHandler;
import ua.ivanzaitsev.bot.handlers.commands.OrderEnterCityCommandHandler;
import ua.ivanzaitsev.bot.handlers.commands.OrderEnterNameCommandHandler;
import ua.ivanzaitsev.bot.handlers.commands.OrderEnterPhoneNumberCommandHandler;
import ua.ivanzaitsev.bot.handlers.commands.OrderStepCancelCommandHandler;
import ua.ivanzaitsev.bot.handlers.commands.OrderStepPreviousCommandHandler;
import ua.ivanzaitsev.bot.handlers.commands.StartCommandHandler;
import ua.ivanzaitsev.bot.handlers.commands.registries.CommandHandlerRegistry;
import ua.ivanzaitsev.bot.handlers.commands.registries.CommandHandlerRegistryDefault;
import ua.ivanzaitsev.bot.repositories.CartRepository;
import ua.ivanzaitsev.bot.repositories.CategoryRepository;
import ua.ivanzaitsev.bot.repositories.ClientActionRepository;
import ua.ivanzaitsev.bot.repositories.ClientCommandStateRepository;
import ua.ivanzaitsev.bot.repositories.ClientOrderStateRepository;
import ua.ivanzaitsev.bot.repositories.ClientRepository;
import ua.ivanzaitsev.bot.repositories.OrderRepository;
import ua.ivanzaitsev.bot.repositories.ProductRepository;
import ua.ivanzaitsev.bot.repositories.database.CategoryRepositoryDefault;
import ua.ivanzaitsev.bot.repositories.database.ClientRepositoryDefault;
import ua.ivanzaitsev.bot.repositories.database.OrderRepositoryDefault;
import ua.ivanzaitsev.bot.repositories.database.ProductRepositoryDefault;
import ua.ivanzaitsev.bot.repositories.memory.CartRepositoryDefault;
import ua.ivanzaitsev.bot.repositories.memory.ClientActionRepositoryDefault;
import ua.ivanzaitsev.bot.repositories.memory.ClientCommandStateRepositoryDefault;
import ua.ivanzaitsev.bot.repositories.memory.ClientOrderStateRepositoryDefault;
import ua.ivanzaitsev.bot.services.MessageService;
import ua.ivanzaitsev.bot.services.NotificationService;
import ua.ivanzaitsev.bot.services.impl.MessageServiceDefault;
import ua.ivanzaitsev.bot.services.impl.NotificationServiceDefault;

public class Application {

    private ConfigReader configReader = ConfigReader.getInstance();

    private ClientActionRepository clientActionRepository;
    private ClientCommandStateRepository clientCommandStateRepository;
    private ClientOrderStateRepository clientOrderStateRepository;
    private CartRepository cartRepository;
    private CategoryRepository categoryRepository;
    private ProductRepository productRepository;
    private OrderRepository orderRepository;
    private ClientRepository clientRepository;

    private MessageService messageService;
    private NotificationService notificationService;

    private CommandHandlerRegistry commandHandlerRegistry;
    private List<CommandHandler> commandHandlers;
    private List<UpdateHandler> updateHandlers;
    private List<ActionHandler> actionHandlers;

    private void initializeRepositories() {
        clientActionRepository = new ClientActionRepositoryDefault();
        clientCommandStateRepository = new ClientCommandStateRepositoryDefault();
        clientOrderStateRepository = new ClientOrderStateRepositoryDefault();
        cartRepository = new CartRepositoryDefault();
        categoryRepository = new CategoryRepositoryDefault();
        productRepository = new ProductRepositoryDefault();
        orderRepository = new OrderRepositoryDefault();
        clientRepository = new ClientRepositoryDefault();
    }

    private void initializeServices() {
        messageService = new MessageServiceDefault();
        notificationService = new NotificationServiceDefault(configReader);
    }

    private void initializeCommandHandlers() {
        commandHandlerRegistry = new CommandHandlerRegistryDefault();
        commandHandlers = new ArrayList<>();

        commandHandlers.add(new CatalogCommandHandler(commandHandlerRegistry, categoryRepository, productRepository,
                cartRepository, messageService));

        commandHandlers.add(new CartCommandHandler(commandHandlerRegistry, clientCommandStateRepository,
                clientOrderStateRepository, cartRepository, clientRepository, messageService));

        commandHandlers.add(new OrderEnterNameCommandHandler(commandHandlerRegistry, clientActionRepository,
                clientCommandStateRepository, clientOrderStateRepository));

        commandHandlers.add(new OrderEnterPhoneNumberCommandHandler(commandHandlerRegistry, clientActionRepository,
                clientCommandStateRepository, clientOrderStateRepository));

        commandHandlers.add(new OrderEnterCityCommandHandler(commandHandlerRegistry, clientActionRepository,
                clientCommandStateRepository, clientOrderStateRepository));

        commandHandlers.add(new OrderEnterAddressCommandHandler(commandHandlerRegistry, clientActionRepository,
                clientCommandStateRepository, clientOrderStateRepository));

        commandHandlers.add(new OrderConfirmCommandHandler(clientActionRepository, clientCommandStateRepository,
                clientOrderStateRepository, cartRepository, orderRepository, clientRepository, messageService,
                notificationService));

        commandHandlerRegistry.setCommandHandlers(commandHandlers);
    }

    private void initializeUpdateHandlers() {
        updateHandlers = new ArrayList<>();

        updateHandlers.add(new StartCommandHandler(clientRepository, messageService));

        updateHandlers.add(new CatalogCommandHandler(commandHandlerRegistry, categoryRepository, productRepository,
                cartRepository, messageService));

        updateHandlers.add(new CartCommandHandler(commandHandlerRegistry, clientCommandStateRepository,
                clientOrderStateRepository, cartRepository, clientRepository, messageService));

        updateHandlers.add(new OrderStepCancelCommandHandler(clientActionRepository, clientCommandStateRepository,
                clientOrderStateRepository));

        updateHandlers.add(new OrderStepPreviousCommandHandler(commandHandlerRegistry, clientCommandStateRepository));

        updateHandlers.add(new OrderEnterPhoneNumberCommandHandler(commandHandlerRegistry, clientActionRepository,
                clientCommandStateRepository, clientOrderStateRepository));
    }

    private void initializeActionHandlers() {
        actionHandlers = new ArrayList<>();

        actionHandlers.add(new OrderEnterNameCommandHandler(commandHandlerRegistry, clientActionRepository,
                clientCommandStateRepository, clientOrderStateRepository));

        actionHandlers.add(new OrderEnterPhoneNumberCommandHandler(commandHandlerRegistry, clientActionRepository,
                clientCommandStateRepository, clientOrderStateRepository));

        actionHandlers.add(new OrderEnterCityCommandHandler(commandHandlerRegistry, clientActionRepository,
                clientCommandStateRepository, clientOrderStateRepository));

        actionHandlers.add(new OrderEnterAddressCommandHandler(commandHandlerRegistry, clientActionRepository,
                clientCommandStateRepository, clientOrderStateRepository));

        actionHandlers.add(new OrderConfirmCommandHandler(clientActionRepository, clientCommandStateRepository,
                clientOrderStateRepository, cartRepository, orderRepository, clientRepository, messageService,
                notificationService));
    }

    public static void main(String[] args) throws TelegramApiException {
        Application application = new Application();
        application.initializeRepositories();
        application.initializeServices();
        application.initializeCommandHandlers();
        application.initializeUpdateHandlers();
        application.initializeActionHandlers();

        TelegramBot telegramBot = new TelegramBot(application.configReader, application.clientActionRepository,
                application.updateHandlers, application.actionHandlers);

        new TelegramBotsApi(DefaultBotSession.class).registerBot(telegramBot);
    }

}

import java.io.Serial;

public class HandlerNotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public HandlerNotFoundException() {
    }

    public HandlerNotFoundException(String message) {
        super(message);
    }

    public HandlerNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}


import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import ua.ivanzaitsev.bot.exceptions.HandlerNotFoundException;
import ua.ivanzaitsev.bot.handlers.ActionHandler;
import ua.ivanzaitsev.bot.handlers.UpdateHandler;
import ua.ivanzaitsev.bot.models.domain.ClientAction;
import ua.ivanzaitsev.bot.models.domain.Command;
import ua.ivanzaitsev.bot.repositories.ClientActionRepository;

public class TelegramBot extends TelegramLongPollingBot {

    private final Logger logger = LogManager.getLogger(getClass());

    private final String telegramBotUsername;
    private final ClientActionRepository clientActionRepository;
    private final Map<Command, UpdateHandler> updateHandlers;
    private final Map<Command, ActionHandler> actionHandlers;

    public TelegramBot(
            ConfigReader configReader,
            ClientActionRepository clientActionRepository,
            List<UpdateHandler> updateHandlers,
            List<ActionHandler> actionHandlers) {

        super(new DefaultBotOptions(), configReader.get("telegram.bot.token"));
        this.telegramBotUsername = configReader.get("telegram.bot.username");
        this.clientActionRepository = clientActionRepository;
        this.updateHandlers = updateHandlers.stream().collect(toMap(UpdateHandler::getCommand, identity()));
        this.actionHandlers = actionHandlers.stream().collect(toMap(ActionHandler::getCommand, identity()));
    }

    @Override
    public String getBotUsername() {
        return telegramBotUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            handle(update);
        } catch (Exception e) {
            logger.error("Failed to handle update", e);
        }
    }

    private void handle(Update update) throws TelegramApiException {
        if (handleCommand(update)) {
            return;
        }
        if (handleAction(update)) {
            return;
        }
    }

    private boolean handleCommand(Update update) throws TelegramApiException {
        List<UpdateHandler> foundCommandHandlers = updateHandlers.values().stream()
                .filter(commandHandler -> commandHandler.canHandleUpdate(update))
                .toList();

        if (foundCommandHandlers.size() > 1) {
            throw new HandlerNotFoundException("Found more than one command handler: " + foundCommandHandlers.size());
        }
        if (foundCommandHandlers.size() != 1) {
            return false;
        }

        foundCommandHandlers.get(0).handleUpdate(this, update);
        return true;
    }

    private boolean handleAction(Update update) throws TelegramApiException {
        if (!update.hasMessage()) {
            return false;
        }

        ClientAction clientAction = clientActionRepository.findByChatId(update.getMessage().getChatId());
        if (clientAction == null) {
            return false;
        }

        ActionHandler actionHandler = actionHandlers.get(clientAction.getCommand());
        if (actionHandler == null) {
            throw new HandlerNotFoundException("Failed to find action handler");
        }

        actionHandler.handleAction(this, update, clientAction.getAction());
        return true;
    }

}


import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigReader {

    private static final ConfigReader INSTANCE = new ConfigReader();

    private final Properties properties;

    private ConfigReader() {
        this.properties = new Properties();
        this.load();
    }

    public static ConfigReader getInstance() {
        return INSTANCE;
    }

    private void load() {
        try (InputStream stream = ConfigReader.class.getClassLoader().getResourceAsStream("application.properties")) {
            properties.load(stream);
        } catch (IOException e) {
            throw new IllegalStateException("Failed load config file", e);
        }
    }

    public String get(String name) {
        return properties.getProperty(name);
    }

}


import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public interface ActionHandler extends Handler {

    boolean canHandleAction(Update update, String action);

    void handleAction(AbsSender absSender, Update update, String action) throws TelegramApiException;

}


import ua.ivanzaitsev.bot.models.domain.Command;

public interface Handler {

    Command getCommand();

}


import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public interface CommandHandler extends Handler {

    void executeCommand(AbsSender absSender, Update update, Long chatId) throws TelegramApiException;

}


import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public interface UpdateHandler extends Handler {

    boolean canHandleUpdate(Update update);

    void handleUpdate(AbsSender absSender, Update update) throws TelegramApiException;

}



import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import ua.ivanzaitsev.bot.handlers.UpdateHandler;
import ua.ivanzaitsev.bot.models.domain.Button;
import ua.ivanzaitsev.bot.models.domain.Command;
import ua.ivanzaitsev.bot.models.entities.Client;
import ua.ivanzaitsev.bot.repositories.ClientRepository;
import ua.ivanzaitsev.bot.services.MessageService;

public class StartCommandHandler implements UpdateHandler {

    private final ClientRepository clientRepository;
    private final MessageService messageService;

    public StartCommandHandler(
            ClientRepository clientRepository,
            MessageService messageService) {

        this.clientRepository = clientRepository;
        this.messageService = messageService;
    }

    @Override
    public Command getCommand() {
        return Command.START;
    }

    @Override
    public boolean canHandleUpdate(Update update) {
        return update.hasMessage() &&
                update.getMessage().hasText() &&
                update.getMessage().getText().startsWith(Button.START.getAlias());
    }

    @Override
    public void handleUpdate(AbsSender absSender, Update update) throws TelegramApiException {
        Long chatId = update.getMessage().getChatId();

        saveClient(chatId);
        sendStartMessage(absSender, chatId);
    }

    private void saveClient(Long chatId) {
        Client client = clientRepository.findByChatId(chatId);

        if (client == null) {
            createClient(chatId);
        } else if (!client.isActive()) {
            activateClient(client);
        }
    }

    private void createClient(Long chatId) {
        Client client = new Client();
        client.setChatId(chatId);
        client.setActive(true);
        clientRepository.save(client);
    }

    private void activateClient(Client client) {
        client.setActive(true);
        clientRepository.update(client);
    }

    private void sendStartMessage(AbsSender absSender, Long chatId) throws TelegramApiException {
        String text = messageService.findByName("START_MESSAGE").buildText();

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .replyMarkup(Button.createGeneralMenuKeyboard())
                .build();
        absSender.execute(message);
    }

}


import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import ua.ivanzaitsev.bot.handlers.CommandHandler;
import ua.ivanzaitsev.bot.handlers.UpdateHandler;
import ua.ivanzaitsev.bot.handlers.commands.registries.CommandHandlerRegistry;
import ua.ivanzaitsev.bot.models.domain.Button;
import ua.ivanzaitsev.bot.models.domain.Command;
import ua.ivanzaitsev.bot.repositories.ClientCommandStateRepository;

public class OrderStepPreviousCommandHandler implements UpdateHandler {

    private final CommandHandlerRegistry commandHandlerRegistry;
    private final ClientCommandStateRepository clientCommandStateRepository;

    public OrderStepPreviousCommandHandler(
            CommandHandlerRegistry commandHandlerRegistry,
            ClientCommandStateRepository clientCommandStateRepository) {

        this.commandHandlerRegistry = commandHandlerRegistry;
        this.clientCommandStateRepository = clientCommandStateRepository;
    }

    @Override
    public Command getCommand() {
        return Command.ORDER_STEP_PREVIOUS;
    }

    @Override
    public boolean canHandleUpdate(Update update) {
        return update.hasMessage() &&
                update.getMessage().hasText() &&
                update.getMessage().getText().startsWith(Button.ORDER_STEP_PREVIOUS.getAlias());
    }

    @Override
    public void handleUpdate(AbsSender absSender, Update update) throws TelegramApiException {
        Long chatId = update.getMessage().getChatId();

        executePreviousCommand(absSender, update, chatId);
    }

    private void executePreviousCommand(AbsSender absSender, Update update, Long chatId) throws TelegramApiException {
        Command command = clientCommandStateRepository.popByChatId(chatId);
        if (command == null) {
            return;
        }

        CommandHandler commandHandler = commandHandlerRegistry.find(command);
        commandHandler.executeCommand(absSender, update, chatId);
    }

}


import java.util.Arrays;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import ua.ivanzaitsev.bot.handlers.ActionHandler;
import ua.ivanzaitsev.bot.handlers.CommandHandler;
import ua.ivanzaitsev.bot.handlers.commands.registries.CommandHandlerRegistry;
import ua.ivanzaitsev.bot.models.domain.Button;
import ua.ivanzaitsev.bot.models.domain.ClientAction;
import ua.ivanzaitsev.bot.models.domain.ClientOrder;
import ua.ivanzaitsev.bot.models.domain.Command;
import ua.ivanzaitsev.bot.repositories.ClientActionRepository;
import ua.ivanzaitsev.bot.repositories.ClientCommandStateRepository;
import ua.ivanzaitsev.bot.repositories.ClientOrderStateRepository;

public class OrderEnterCityCommandHandler implements CommandHandler, ActionHandler {

    private static final String ENTER_CITY_ACTION = "order=enter-client-city";

    private static final Pattern CITY_PATTERN = Pattern.compile("[a-zA-Z]");

    private final CommandHandlerRegistry commandHandlerRegistry;
    private final ClientActionRepository clientActionRepository;
    private final ClientCommandStateRepository clientCommandStateRepository;
    private final ClientOrderStateRepository clientOrderStateRepository;

    public OrderEnterCityCommandHandler(
            CommandHandlerRegistry commandHandlerRegistry,
            ClientActionRepository clientActionRepository,
            ClientCommandStateRepository clientCommandStateRepository,
            ClientOrderStateRepository clientOrderStateRepository) {

        this.commandHandlerRegistry = commandHandlerRegistry;
        this.clientActionRepository = clientActionRepository;
        this.clientCommandStateRepository = clientCommandStateRepository;
        this.clientOrderStateRepository = clientOrderStateRepository;
    }

    @Override
    public Command getCommand() {
        return Command.ENTER_CITY;
    }

    @Override
    public void executeCommand(AbsSender absSender, Update update, Long chatId) throws TelegramApiException {
        clientActionRepository.updateByChatId(chatId, new ClientAction(getCommand(), ENTER_CITY_ACTION));

        sendEnterCityMessage(absSender, chatId);
        sendCurrentCityMessage(absSender, chatId);
    }

    private void sendEnterCityMessage(AbsSender absSender, Long chatId) throws TelegramApiException {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Enter your city")
                .replyMarkup(buildReplyKeyboardMarkup(false))
                .build();
        absSender.execute(message);
    }

    private void sendCurrentCityMessage(AbsSender absSender, Long chatId) throws TelegramApiException {
        ClientOrder clientOrder = clientOrderStateRepository.findByChatId(chatId);
        if (StringUtils.isBlank(clientOrder.getCity())) {
            return;
        }

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Current city: " + clientOrder.getCity())
                .replyMarkup(buildReplyKeyboardMarkup(true))
                .build();
        absSender.execute(message);
    }

    private ReplyKeyboardMarkup buildReplyKeyboardMarkup(boolean skip) {
        ReplyKeyboardMarkup.ReplyKeyboardMarkupBuilder keyboardBuilder = ReplyKeyboardMarkup.builder();
        keyboardBuilder.resizeKeyboard(true);
        keyboardBuilder.selective(true);

        if (skip) {
            keyboardBuilder.keyboardRow(new KeyboardRow(Arrays.asList(
                    KeyboardButton.builder().text(Button.ORDER_STEP_NEXT.getAlias()).build()
                    )));
        }

        keyboardBuilder.keyboardRow(new KeyboardRow(Arrays.asList(
                KeyboardButton.builder().text(Button.ORDER_STEP_CANCEL.getAlias()).build(),
                KeyboardButton.builder().text(Button.ORDER_STEP_PREVIOUS.getAlias()).build()
                )));
        return keyboardBuilder.build();
    }

    @Override
    public boolean canHandleAction(Update update, String action) {
        return update.hasMessage() && update.getMessage().hasText() && ENTER_CITY_ACTION.equals(action);
    }

    @Override
    public void handleAction(AbsSender absSender, Update update, String action) throws TelegramApiException {
        Long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();

        if (Button.ORDER_STEP_NEXT.getAlias().equals(text)) {
            executeNextCommand(absSender, update, chatId);
            return;
        }
        if (!CITY_PATTERN.matcher(text).find()) {
            sendNotCorrectCityMessage(absSender, chatId);
            return;
        }

        saveClientOrderState(chatId, text);
        executeNextCommand(absSender, update, chatId);
    }

    private void sendNotCorrectCityMessage(AbsSender absSender, Long chatId) throws TelegramApiException {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("You entered the incorrect city, try again.")
                .build();
        absSender.execute(message);
    }

    private void saveClientOrderState(Long chatId, String text) {
        ClientOrder clientOrder = clientOrderStateRepository.findByChatId(chatId);
        clientOrder.setCity(text);
        clientOrderStateRepository.updateByChatId(chatId, clientOrder);
    }

    private void executeNextCommand(AbsSender absSender, Update update, Long chatId) throws TelegramApiException {
        clientCommandStateRepository.pushByChatId(chatId, getCommand());
        commandHandlerRegistry.find(Command.ENTER_ADDRESS).executeCommand(absSender, update, chatId);
    }

}


import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import ua.ivanzaitsev.bot.handlers.UpdateHandler;
import ua.ivanzaitsev.bot.models.domain.Button;
import ua.ivanzaitsev.bot.models.domain.Command;
import ua.ivanzaitsev.bot.repositories.ClientActionRepository;
import ua.ivanzaitsev.bot.repositories.ClientCommandStateRepository;
import ua.ivanzaitsev.bot.repositories.ClientOrderStateRepository;

public class OrderStepCancelCommandHandler implements UpdateHandler {

    private final ClientActionRepository clientActionRepository;
    private final ClientCommandStateRepository clientCommandStateRepository;
    private final ClientOrderStateRepository clientOrderStateRepository;

    public OrderStepCancelCommandHandler(
            ClientActionRepository clientActionRepository,
            ClientCommandStateRepository clientCommandStateRepository,
            ClientOrderStateRepository clientOrderStateRepository) {

        this.clientActionRepository = clientActionRepository;
        this.clientCommandStateRepository = clientCommandStateRepository;
        this.clientOrderStateRepository = clientOrderStateRepository;
    }

    @Override
    public Command getCommand() {
        return Command.ORDER_STEP_CANCEL;
    }

    @Override
    public boolean canHandleUpdate(Update update) {
        return update.hasMessage() &&
                update.getMessage().hasText() &&
                update.getMessage().getText().startsWith(Button.ORDER_STEP_CANCEL.getAlias());
    }

    @Override
    public void handleUpdate(AbsSender absSender, Update update) throws TelegramApiException {
        Long chatId = update.getMessage().getChatId();

        clearClientOrderState(chatId);
        sendCanclelOrderMessage(absSender, chatId);
    }

    private void clearClientOrderState(Long chatId) {
        clientActionRepository.deleteByChatId(chatId);
        clientCommandStateRepository.deleteAllByChatId(chatId);
        clientOrderStateRepository.deleteByChatId(chatId);
    }

    private void sendCanclelOrderMessage(AbsSender absSender, Long chatId) throws TelegramApiException {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Order canceled.")
                .replyMarkup(Button.createGeneralMenuKeyboard())
                .build();
        absSender.execute(message);
    }

}


import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import ua.ivanzaitsev.bot.handlers.CommandHandler;
import ua.ivanzaitsev.bot.handlers.UpdateHandler;
import ua.ivanzaitsev.bot.handlers.commands.registries.CommandHandlerRegistry;
import ua.ivanzaitsev.bot.models.domain.Button;
import ua.ivanzaitsev.bot.models.domain.CartItem;
import ua.ivanzaitsev.bot.models.domain.ClientOrder;
import ua.ivanzaitsev.bot.models.domain.Command;
import ua.ivanzaitsev.bot.models.domain.MessagePlaceholder;
import ua.ivanzaitsev.bot.models.entities.Client;
import ua.ivanzaitsev.bot.models.entities.Message;
import ua.ivanzaitsev.bot.models.entities.Product;
import ua.ivanzaitsev.bot.repositories.CartRepository;
import ua.ivanzaitsev.bot.repositories.ClientCommandStateRepository;
import ua.ivanzaitsev.bot.repositories.ClientOrderStateRepository;
import ua.ivanzaitsev.bot.repositories.ClientRepository;
import ua.ivanzaitsev.bot.services.MessageService;

public class CartCommandHandler implements CommandHandler, UpdateHandler {

    private static final int MAX_QUANTITY_PER_PRODUCT = 50;

    private static final String PRODUCT_QUANTITY_CALLBACK = "cart=product-quantity";
    private static final String CURRENT_PAGE_CALLBACK = "cart=current-page";
    private static final String DELETE_PRODUCT_CALLBACK = "cart=delete-product";
    private static final String MINUS_PRODUCT_CALLBACK = "cart=minus-product";
    private static final String PLUS_PRODUCT_CALLBACK = "cart=plus-product";
    private static final String PREVIOUS_PRODUCT_CALLBACK = "cart=previous-product";
    private static final String NEXT_PRODUCT_CALLBACK = "cart=next-product";
    private static final String PROCESS_ORDER_CALLBACK = "cart=process-order";

    private static final Set<String> CALLBACKS = Set.of(DELETE_PRODUCT_CALLBACK, MINUS_PRODUCT_CALLBACK,
            PLUS_PRODUCT_CALLBACK, PREVIOUS_PRODUCT_CALLBACK, NEXT_PRODUCT_CALLBACK, PROCESS_ORDER_CALLBACK);

    private final CommandHandlerRegistry commandHandlerRegistry;
    private final ClientCommandStateRepository clientCommandStateRepository;
    private final ClientOrderStateRepository clientOrderStateRepository;
    private final CartRepository cartRepository;
    private final ClientRepository clientRepository;
    private final MessageService messageService;

    public CartCommandHandler(
            CommandHandlerRegistry commandHandlerRegistry,
            ClientCommandStateRepository clientCommandStateRepository,
            ClientOrderStateRepository clientOrderStateRepository,
            CartRepository cartRepository,
            ClientRepository clientRepository,
            MessageService messageService) {

        this.commandHandlerRegistry = commandHandlerRegistry;
        this.clientCommandStateRepository = clientCommandStateRepository;
        this.clientOrderStateRepository = clientOrderStateRepository;
        this.cartRepository = cartRepository;
        this.clientRepository = clientRepository;
        this.messageService = messageService;
    }

    @Override
    public Command getCommand() {
        return Command.CART;
    }

    @Override
    public void executeCommand(AbsSender absSender, Update update, Long chatId) throws TelegramApiException {
        handleCartMessageUpdate(absSender, chatId);
    }

    @Override
    public boolean canHandleUpdate(Update update) {
        return isCartMessageUpdate(update) || isCallbackQueryUpdate(update);
    }

    @Override
    public void handleUpdate(AbsSender absSender, Update update) throws TelegramApiException {
        if (isCartMessageUpdate(update)) {
            handleCartMessageUpdate(absSender, update.getMessage().getChatId());
        }
        if (isCallbackQueryUpdate(update)) {
            handleCallbackQueryUpdate(absSender, update);
        }
    }

    private boolean isCartMessageUpdate(Update update) {
        return update.hasMessage() &&
                update.getMessage().hasText() &&
                update.getMessage().getText().equals(Button.CART.getAlias());
    }

    private boolean isCallbackQueryUpdate(Update update) {
        return update.hasCallbackQuery() && CALLBACKS.contains(update.getCallbackQuery().getData());
    }

    private void handleCartMessageUpdate(AbsSender absSender, Long chatId) throws TelegramApiException {
        List<CartItem> cartItems = cartRepository.findAllCartItemsByChatId(chatId);
        cartRepository.updatePageNumberByChatId(chatId, 0);

        if (cartItems.isEmpty()) {
            sendEmptyCartMessage(absSender, chatId);
            return;
        }

        sendCartMessage(absSender, chatId, cartItems, 0);
    }

    private void handleCallbackQueryUpdate(AbsSender absSender, Update update) throws TelegramApiException {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();
        String data = callbackQuery.getData();

        if (DELETE_PRODUCT_CALLBACK.equals(data)) {
            doDeleteProduct(absSender, chatId, messageId);
        }
        if (MINUS_PRODUCT_CALLBACK.equals(data)) {
            doMinusProduct(absSender, chatId, messageId);
        }
        if (PLUS_PRODUCT_CALLBACK.equals(data)) {
            doPlusProduct(absSender, chatId, messageId);
        }
        if (PREVIOUS_PRODUCT_CALLBACK.equals(data)) {
            doPreviousProduct(absSender, chatId, messageId);
        }
        if (NEXT_PRODUCT_CALLBACK.equals(data)) {
            doNextProduct(absSender, chatId, messageId);
        }
        if (PROCESS_ORDER_CALLBACK.equals(data)) {
            doProcessOrder(absSender, update, chatId, messageId);
        }
    }

    private void doDeleteProduct(AbsSender absSender, Long chatId, Integer messageId) throws TelegramApiException {
        List<CartItem> cartItems = cartRepository.findAllCartItemsByChatId(chatId);
        int currentCartPage = cartRepository.findPageNumberByChatId(chatId);

        if (!cartItems.isEmpty()) {
            CartItem cartItem = cartItems.get(currentCartPage);
            if (cartItem != null) {
                cartItems.remove(cartItem);
                cartRepository.deleteCartItem(chatId, cartItem.getId());
            }
        }

        if (cartItems.isEmpty()) {
            editClearedCartMessage(absSender, chatId, messageId);
            return;
        }

        if (cartItems.size() == currentCartPage) {
            currentCartPage -= 1;
            cartRepository.updatePageNumberByChatId(chatId, currentCartPage);
        }

        editCartMessage(absSender, chatId, messageId, cartItems, currentCartPage);
    }

    private void doMinusProduct(AbsSender absSender, Long chatId, Integer messageId) throws TelegramApiException {
        List<CartItem> cartItems = cartRepository.findAllCartItemsByChatId(chatId);
        int currentCartPage = cartRepository.findPageNumberByChatId(chatId);

        if (cartItems.isEmpty()) {
            editEmptyCartMessage(absSender, chatId, messageId);
            return;
        }

        CartItem cartItem = cartItems.get(currentCartPage);

        if (cartItem == null || cartItem.getQuantity() <= 1) {
            return;
        }

        cartItem.setQuantity(cartItem.getQuantity() - 1);
        cartRepository.updateCartItem(chatId, cartItem);

        editCartMessage(absSender, chatId, messageId, cartItems, currentCartPage);
    }

    private void doPlusProduct(AbsSender absSender, Long chatId, Integer messageId) throws TelegramApiException {
        List<CartItem> cartItems = cartRepository.findAllCartItemsByChatId(chatId);
        int currentCartPage = cartRepository.findPageNumberByChatId(chatId);

        if (cartItems.isEmpty()) {
            editEmptyCartMessage(absSender, chatId, messageId);
            return;
        }

        CartItem cartItem = cartItems.get(currentCartPage);

        if (cartItem == null || cartItem.getQuantity() >= MAX_QUANTITY_PER_PRODUCT) {
            return;
        }

        cartItem.setQuantity(cartItem.getQuantity() + 1);
        cartRepository.updateCartItem(chatId, cartItem);

        editCartMessage(absSender, chatId, messageId, cartItems, currentCartPage);
    }

    private void doPreviousProduct(AbsSender absSender, Long chatId, Integer messageId) throws TelegramApiException {
        List<CartItem> cartItems = cartRepository.findAllCartItemsByChatId(chatId);
        int currentCartPage = cartRepository.findPageNumberByChatId(chatId);

        if (cartItems.isEmpty()) {
            editEmptyCartMessage(absSender, chatId, messageId);
            return;
        }
        if (cartItems.size() == 1) {
            return;
        }

        if (currentCartPage <= 0) {
            currentCartPage = cartItems.size() - 1;
        } else {
            currentCartPage -= 1;
        }
        cartRepository.updatePageNumberByChatId(chatId, currentCartPage);

        editCartMessage(absSender, chatId, messageId, cartItems, currentCartPage);
    }

    private void doNextProduct(AbsSender absSender, Long chatId, Integer messageId) throws TelegramApiException {
        List<CartItem> cartItems = cartRepository.findAllCartItemsByChatId(chatId);
        int currentCartPage = cartRepository.findPageNumberByChatId(chatId);

        if (cartItems.isEmpty()) {
            editEmptyCartMessage(absSender, chatId, messageId);
            return;
        }
        if (cartItems.size() == 1) {
            return;
        }

        if (currentCartPage >= cartItems.size() - 1) {
            currentCartPage = 0;
        } else {
            currentCartPage += 1;
        }
        cartRepository.updatePageNumberByChatId(chatId, currentCartPage);

        editCartMessage(absSender, chatId, messageId, cartItems, currentCartPage);
    }

    private void sendEmptyCartMessage(AbsSender absSender, Long chatId) throws TelegramApiException {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Cart is empty.")
                .build();
        absSender.execute(message);
    }

    private void editEmptyCartMessage(AbsSender absSender, Long chatId, Integer messageId) throws TelegramApiException {
        EditMessageText message = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text("Cart is empty.")
                .build();
        absSender.execute(message);
    }

    private void editClearedCartMessage(AbsSender absSender, Long chatId, Integer messageId) throws TelegramApiException {
        EditMessageText message = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text("Cart cleared.")
                .build();
        absSender.execute(message);
    }

    private void sendCartMessage(AbsSender absSender, Long chatId, List<CartItem> cartItems, int currentCartPage)
            throws TelegramApiException {

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(createProductText(cartItems.get(currentCartPage)))
                .replyMarkup(createCartKeyboard(cartItems, currentCartPage))
                .parseMode("HTML")
                .build();
        absSender.execute(message);
    }

    private void editCartMessage(AbsSender absSender, Long chatId, Integer messageId, List<CartItem> cartItems,
            int currentCartPage) throws TelegramApiException {

        EditMessageText message = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(createProductText(cartItems.get(currentCartPage)))
                .replyMarkup(createCartKeyboard(cartItems, currentCartPage))
                .parseMode("HTML")
                .build();
        absSender.execute(message);
    }

    private String createProductText(CartItem cartItem) {
        Message message = messageService.findByName("CART_MESSAGE");
        if (cartItem != null) {
            Product product = cartItem.getProduct();
            message.applyPlaceholder(MessagePlaceholder.of("%PRODUCT_NAME%", product.getName()));
            message.applyPlaceholder(MessagePlaceholder.of("%PRODUCT_DESCRIPTION%", product.getDescription()));
            message.applyPlaceholder(MessagePlaceholder.of("%PRODUCT_PRICE%", product.getPrice()));
            message.applyPlaceholder(MessagePlaceholder.of("%PRODUCT_QUANTITY%", cartItem.getQuantity()));
            message.applyPlaceholder(
                    MessagePlaceholder.of("%PRODUCT_TOTAL_PRICE%", product.getPrice() * cartItem.getQuantity()));
        }
        return message.buildText();
    }

    private InlineKeyboardMarkup createCartKeyboard(List<CartItem> cartItems, int currentCartPage) {
        ClientOrder clientOrder = new ClientOrder();
        clientOrder.setCartItems(cartItems);

        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder keyboardBuilder = InlineKeyboardMarkup.builder();

        keyboardBuilder.keyboardRow(Arrays.asList(
                InlineKeyboardButton.builder().text("\u2716").callbackData(DELETE_PRODUCT_CALLBACK).build(),
                InlineKeyboardButton.builder().text("\u2796").callbackData(MINUS_PRODUCT_CALLBACK).build(),
                InlineKeyboardButton.builder().text(cartItems.get(currentCartPage).getQuantity() + " pcs.")
                    .callbackData(PRODUCT_QUANTITY_CALLBACK).build(),
                InlineKeyboardButton.builder().text("\u2795").callbackData(PLUS_PRODUCT_CALLBACK).build()
                ));

        keyboardBuilder.keyboardRow(Arrays.asList(
                InlineKeyboardButton.builder().text("\u25c0").callbackData(PREVIOUS_PRODUCT_CALLBACK).build(),
                InlineKeyboardButton.builder().text((currentCartPage + 1) + "/" + cartItems.size())
                    .callbackData(CURRENT_PAGE_CALLBACK).build(),
                InlineKeyboardButton.builder().text("\u25b6").callbackData(NEXT_PRODUCT_CALLBACK).build()
                ));

        keyboardBuilder.keyboardRow(Arrays.asList(
                InlineKeyboardButton.builder()
                    .text(String.format("\u2705 Order for %d $ Checkout?", clientOrder.calculateTotalPrice()))
                    .callbackData(PROCESS_ORDER_CALLBACK).build()
                ));
        return keyboardBuilder.build();
    }

    private void doProcessOrder(AbsSender absSender, Update update, Long chatId, Integer messageId)
            throws TelegramApiException {

        List<CartItem> cartItems = cartRepository.findAllCartItemsByChatId(chatId);
        if (cartItems.isEmpty()) {
            editEmptyCartMessage(absSender, chatId, messageId);
            return;
        }

        editProcessOrderMessage(absSender, chatId, messageId);
        saveClientOrderState(chatId, cartItems);
        executeNextCommand(absSender, update, chatId);
    }

    private void saveClientOrderState(Long chatId, List<CartItem> cartItems) {
        Client client = clientRepository.findByChatId(chatId);

        ClientOrder clientOrder = new ClientOrder();
        clientOrder.setCartItems(cartItems);
        clientOrder.setClientName(client.getName());
        clientOrder.setPhoneNumber(client.getPhoneNumber());
        clientOrder.setCity(client.getCity());
        clientOrder.setAddress(client.getAddress());
        clientOrderStateRepository.updateByChatId(chatId, clientOrder);
    }

    private void editProcessOrderMessage(AbsSender absSender, Long chatId, Integer messageId)
            throws TelegramApiException {

        EditMessageText message = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text("Creating order...")
                .build();
        absSender.execute(message);
    }

    private void executeNextCommand(AbsSender absSender, Update update, Long chatId) throws TelegramApiException {
        clientCommandStateRepository.pushByChatId(chatId, getCommand());
        commandHandlerRegistry.find(Command.ENTER_NAME).executeCommand(absSender, update, chatId);
    }

}


import java.time.LocalDateTime;
import java.util.Arrays;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import ua.ivanzaitsev.bot.handlers.ActionHandler;
import ua.ivanzaitsev.bot.handlers.CommandHandler;
import ua.ivanzaitsev.bot.models.domain.Button;
import ua.ivanzaitsev.bot.models.domain.ClientAction;
import ua.ivanzaitsev.bot.models.domain.ClientOrder;
import ua.ivanzaitsev.bot.models.domain.Command;
import ua.ivanzaitsev.bot.models.entities.Client;
import ua.ivanzaitsev.bot.models.entities.Order;
import ua.ivanzaitsev.bot.models.entities.OrderItem;
import ua.ivanzaitsev.bot.models.entities.OrderStatus;
import ua.ivanzaitsev.bot.repositories.CartRepository;
import ua.ivanzaitsev.bot.repositories.ClientActionRepository;
import ua.ivanzaitsev.bot.repositories.ClientCommandStateRepository;
import ua.ivanzaitsev.bot.repositories.ClientOrderStateRepository;
import ua.ivanzaitsev.bot.repositories.ClientRepository;
import ua.ivanzaitsev.bot.repositories.OrderRepository;
import ua.ivanzaitsev.bot.repositories.hibernate.HibernateTransactionFactory;
import ua.ivanzaitsev.bot.services.MessageService;
import ua.ivanzaitsev.bot.services.NotificationService;

public class OrderConfirmCommandHandler implements CommandHandler, ActionHandler {

    private static final String CONFIRM_ORDER_ACTION = "order=confirm";

    private final ClientActionRepository clientActionRepository;
    private final ClientCommandStateRepository clientCommandStateRepository;
    private final ClientOrderStateRepository clientOrderStateRepository;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final ClientRepository clientRepository;
    private final MessageService messageService;
    private final NotificationService notificationService;

    public OrderConfirmCommandHandler(
            ClientActionRepository clientActionRepository,
            ClientCommandStateRepository clientCommandStateRepository,
            ClientOrderStateRepository clientOrderStateRepository,
            CartRepository cartRepository,
            OrderRepository orderRepository,
            ClientRepository clientRepository,
            MessageService messageService,
            NotificationService notificationService) {

        this.clientActionRepository = clientActionRepository;
        this.clientCommandStateRepository = clientCommandStateRepository;
        this.clientOrderStateRepository = clientOrderStateRepository;
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
        this.clientRepository = clientRepository;
        this.messageService = messageService;
        this.notificationService = notificationService;
    }

    @Override
    public Command getCommand() {
        return Command.ORDER_CONFIRM;
    }

    @Override
    public void executeCommand(AbsSender absSender, Update update, Long chatId) throws TelegramApiException {
        clientActionRepository.updateByChatId(chatId, new ClientAction(getCommand(), CONFIRM_ORDER_ACTION));

        sendConfirmOrderMessage(absSender, chatId);
    }

    private void sendConfirmOrderMessage(AbsSender absSender, Long chatId) throws TelegramApiException {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .parseMode("HTML")
                .text("Confirm order:")
                .replyMarkup(buildReplyKeyboardMarkup())
                .build();
        absSender.execute(message);
    }

    private ReplyKeyboardMarkup buildReplyKeyboardMarkup() {
        ReplyKeyboardMarkup.ReplyKeyboardMarkupBuilder keyboardBuilder = ReplyKeyboardMarkup.builder();
        keyboardBuilder.resizeKeyboard(true);
        keyboardBuilder.selective(true);

        keyboardBuilder.keyboardRow(new KeyboardRow(Arrays.asList(
                KeyboardButton.builder().text(Button.ORDER_CONFIRM.getAlias()).build()
                )));

        keyboardBuilder.keyboardRow(new KeyboardRow(Arrays.asList(
                KeyboardButton.builder().text(Button.ORDER_STEP_CANCEL.getAlias()).build(),
                KeyboardButton.builder().text(Button.ORDER_STEP_PREVIOUS.getAlias()).build()
                )));
        return keyboardBuilder.build();
    }

    @Override
    public boolean canHandleAction(Update update, String action) {
        return update.hasMessage() && update.getMessage().hasText() && CONFIRM_ORDER_ACTION.equals(action);
    }

    @Override
    public void handleAction(AbsSender absSender, Update update, String action) throws TelegramApiException {
        Long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();

        if (!Button.ORDER_CONFIRM.getAlias().equals(text)) {
            sendNotCorrectConfirmOptionMessage(absSender, chatId);
            return;
        }

        completeOrder(absSender, update, chatId);
    }

    private void sendNotCorrectConfirmOptionMessage(AbsSender absSender, Long chatId) throws TelegramApiException {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("You have selected incorrect confirmation option, please press the button")
                .build();
        absSender.execute(message);
    }

    private void completeOrder(AbsSender absSender, Update update, Long chatId) throws TelegramApiException {
        ClientOrder clientOrder = clientOrderStateRepository.findByChatId(chatId);

        Client client = clientRepository.findByChatId(chatId);
        client.setName(clientOrder.getClientName());
        client.setPhoneNumber(clientOrder.getPhoneNumber());
        client.setCity(clientOrder.getCity());
        client.setAddress(clientOrder.getAddress());

        Order order = new Order();
        order.setClient(client);
        order.setCreatedDate(LocalDateTime.now());
        order.setStatus(OrderStatus.WAITING);
        order.setAmount(clientOrder.calculateTotalPrice());
        order.setItems(clientOrder.getCartItems().stream().map(OrderItem::from).toList());

        HibernateTransactionFactory.inTransactionVoid(session -> {
            orderRepository.save(order);
            clientRepository.update(order.getClient());
        });

        sendOrderMessage(absSender, chatId);
        clearClientOrderState(chatId);

        notificationService.notifyAdminChatAboutNewOrder(absSender, order);
    }

    private void sendOrderMessage(AbsSender absSender, Long chatId) throws TelegramApiException {
        String text = messageService.findByName("ORDER_CREATED_MESSAGE").buildText();

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .replyMarkup(Button.createGeneralMenuKeyboard())
                .build();
        absSender.execute(message);
    }

    private void clearClientOrderState(Long chatId) {
        clientActionRepository.deleteByChatId(chatId);
        clientCommandStateRepository.deleteAllByChatId(chatId);
        clientOrderStateRepository.deleteByChatId(chatId);
        cartRepository.deleteAllCartItemsByChatId(chatId);
    }

}


import java.util.Arrays;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import ua.ivanzaitsev.bot.handlers.ActionHandler;
import ua.ivanzaitsev.bot.handlers.CommandHandler;
import ua.ivanzaitsev.bot.handlers.commands.registries.CommandHandlerRegistry;
import ua.ivanzaitsev.bot.models.domain.Button;
import ua.ivanzaitsev.bot.models.domain.ClientAction;
import ua.ivanzaitsev.bot.models.domain.ClientOrder;
import ua.ivanzaitsev.bot.models.domain.Command;
import ua.ivanzaitsev.bot.repositories.ClientActionRepository;
import ua.ivanzaitsev.bot.repositories.ClientCommandStateRepository;
import ua.ivanzaitsev.bot.repositories.ClientOrderStateRepository;

public class OrderEnterAddressCommandHandler implements CommandHandler, ActionHandler {

    private static final String ENTER_ADDRESS_ACTION = "order=enter-client-address";

    private static final Pattern ADDRESS_PATTERN = Pattern.compile("[a-zA-Z]");

    private final CommandHandlerRegistry commandHandlerRegistry;
    private final ClientActionRepository clientActionRepository;
    private final ClientCommandStateRepository clientCommandStateRepository;
    private final ClientOrderStateRepository clientOrderStateRepository;

    public OrderEnterAddressCommandHandler(
            CommandHandlerRegistry commandHandlerRegistry,
            ClientActionRepository clientActionRepository,
            ClientCommandStateRepository clientCommandStateRepository,
            ClientOrderStateRepository clientOrderStateRepository) {

        this.commandHandlerRegistry = commandHandlerRegistry;
        this.clientActionRepository = clientActionRepository;
        this.clientCommandStateRepository = clientCommandStateRepository;
        this.clientOrderStateRepository = clientOrderStateRepository;
    }

    @Override
    public Command getCommand() {
        return Command.ENTER_ADDRESS;
    }

    @Override
    public void executeCommand(AbsSender absSender, Update update, Long chatId) throws TelegramApiException {
        clientActionRepository.updateByChatId(chatId, new ClientAction(getCommand(), ENTER_ADDRESS_ACTION));

        sendEnterAddressMessage(absSender, chatId);
        sendCurrentAddressMessage(absSender, chatId);
    }

    private void sendEnterAddressMessage(AbsSender absSender, Long chatId) throws TelegramApiException {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Enter your address")
                .replyMarkup(buildReplyKeyboardMarkup(false))
                .build();
        absSender.execute(message);
    }

    private void sendCurrentAddressMessage(AbsSender absSender, Long chatId) throws TelegramApiException {
        ClientOrder clientOrder = clientOrderStateRepository.findByChatId(chatId);
        if (StringUtils.isBlank(clientOrder.getAddress())) {
            return;
        }

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Current address: " + clientOrder.getAddress())
                .replyMarkup(buildReplyKeyboardMarkup(true))
                .build();
        absSender.execute(message);
    }

    private ReplyKeyboardMarkup buildReplyKeyboardMarkup(boolean skip) {
        ReplyKeyboardMarkup.ReplyKeyboardMarkupBuilder keyboardBuilder = ReplyKeyboardMarkup.builder();
        keyboardBuilder.resizeKeyboard(true);
        keyboardBuilder.selective(true);

        if (skip) {
            keyboardBuilder.keyboardRow(new KeyboardRow(Arrays.asList(
                    KeyboardButton.builder().text(Button.ORDER_STEP_NEXT.getAlias()).build()
                    )));
        }

        keyboardBuilder.keyboardRow(new KeyboardRow(Arrays.asList(
                KeyboardButton.builder().text(Button.ORDER_STEP_CANCEL.getAlias()).build(),
                KeyboardButton.builder().text(Button.ORDER_STEP_PREVIOUS.getAlias()).build()
                )));
        return keyboardBuilder.build();
    }

    @Override
    public boolean canHandleAction(Update update, String action) {
        return update.hasMessage() && update.getMessage().hasText() && ENTER_ADDRESS_ACTION.equals(action);
    }

    @Override
    public void handleAction(AbsSender absSender, Update update, String action) throws TelegramApiException {
        Long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();

        if (Button.ORDER_STEP_NEXT.getAlias().equals(text)) {
            executeNextCommand(absSender, update, chatId);
            return;
        }
        if (!ADDRESS_PATTERN.matcher(text).find()) {
            sendNotCorrectAddressMessage(absSender, chatId);
            return;
        }

        saveClientOrderState(chatId, text);
        executeNextCommand(absSender, update, chatId);
    }

    private void sendNotCorrectAddressMessage(AbsSender absSender, Long chatId) throws TelegramApiException {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("You entered the incorrect address, try again.")
                .build();
        absSender.execute(message);
    }

    private void saveClientOrderState(Long chatId, String text) {
        ClientOrder clientOrder = clientOrderStateRepository.findByChatId(chatId);
        clientOrder.setAddress(text);
        clientOrderStateRepository.updateByChatId(chatId, clientOrder);
    }

    private void executeNextCommand(AbsSender absSender, Update update, Long chatId) throws TelegramApiException {
        clientCommandStateRepository.pushByChatId(chatId, getCommand());
        commandHandlerRegistry.find(Command.ORDER_CONFIRM).executeCommand(absSender, update, chatId);
    }

}


import java.util.Arrays;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import ua.ivanzaitsev.bot.handlers.ActionHandler;
import ua.ivanzaitsev.bot.handlers.CommandHandler;
import ua.ivanzaitsev.bot.handlers.UpdateHandler;
import ua.ivanzaitsev.bot.handlers.commands.registries.CommandHandlerRegistry;
import ua.ivanzaitsev.bot.models.domain.Button;
import ua.ivanzaitsev.bot.models.domain.ClientAction;
import ua.ivanzaitsev.bot.models.domain.ClientOrder;
import ua.ivanzaitsev.bot.models.domain.Command;
import ua.ivanzaitsev.bot.repositories.ClientActionRepository;
import ua.ivanzaitsev.bot.repositories.ClientCommandStateRepository;
import ua.ivanzaitsev.bot.repositories.ClientOrderStateRepository;

public class OrderEnterPhoneNumberCommandHandler implements CommandHandler, UpdateHandler, ActionHandler {

    private static final String ENTER_PHONE_NUMBER_ACTION = "order=enter-client-phone-number";

    private static final Pattern PHONE_NUMBER_PATTERN =
            Pattern.compile("^(\\s*)?(\\+)?([- _():=+]?\\d[- _():=+]?){10,14}(\\s*)?$");

    private final CommandHandlerRegistry commandHandlerRegistry;
    private final ClientActionRepository clientActionRepository;
    private final ClientCommandStateRepository clientCommandStateRepository;
    private final ClientOrderStateRepository clientOrderStateRepository;

    public OrderEnterPhoneNumberCommandHandler(
            CommandHandlerRegistry commandHandlerRegistry,
            ClientActionRepository clientActionRepository,
            ClientCommandStateRepository clientCommandStateRepository,
            ClientOrderStateRepository clientOrderStateRepository) {

        this.commandHandlerRegistry = commandHandlerRegistry;
        this.clientActionRepository = clientActionRepository;
        this.clientCommandStateRepository = clientCommandStateRepository;
        this.clientOrderStateRepository = clientOrderStateRepository;
    }

    @Override
    public Command getCommand() {
        return Command.ENTER_PHONE_NUMBER;
    }

    @Override
    public void executeCommand(AbsSender absSender, Update update, Long chatId) throws TelegramApiException {
        clientActionRepository.updateByChatId(chatId, new ClientAction(getCommand(), ENTER_PHONE_NUMBER_ACTION));

        sendEnterPhoneNumberMessage(absSender, chatId);
        sendCurrentPhoneNumberMessage(absSender, chatId);
    }

    private void sendEnterPhoneNumberMessage(AbsSender absSender, Long chatId) throws TelegramApiException {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Enter your phone number or press button")
                .replyMarkup(buildReplyKeyboardMarkup(false))
                .build();
        absSender.execute(message);
    }

    private void sendCurrentPhoneNumberMessage(AbsSender absSender, Long chatId) throws TelegramApiException {
        ClientOrder clientOrder = clientOrderStateRepository.findByChatId(chatId);
        if (StringUtils.isBlank(clientOrder.getPhoneNumber())) {
            return;
        }

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Current phone number: " + clientOrder.getPhoneNumber())
                .replyMarkup(buildReplyKeyboardMarkup(true))
                .build();
        absSender.execute(message);
    }

    private ReplyKeyboardMarkup buildReplyKeyboardMarkup(boolean skip) {
        ReplyKeyboardMarkup.ReplyKeyboardMarkupBuilder keyboardBuilder = ReplyKeyboardMarkup.builder();
        keyboardBuilder.resizeKeyboard(true);
        keyboardBuilder.selective(true);

        if (skip) {
            keyboardBuilder.keyboardRow(new KeyboardRow(Arrays.asList(
                    KeyboardButton.builder().text(Button.ORDER_STEP_NEXT.getAlias()).build()
                    )));
        }

        keyboardBuilder.keyboardRow(new KeyboardRow(Arrays.asList(
                KeyboardButton.builder().text(Button.SEND_PHONE_NUMBER.getAlias()).requestContact(true).build()
                )));

        keyboardBuilder.keyboardRow(new KeyboardRow(Arrays.asList(
                KeyboardButton.builder().text(Button.ORDER_STEP_CANCEL.getAlias()).build(),
                KeyboardButton.builder().text(Button.ORDER_STEP_PREVIOUS.getAlias()).build()
                )));
        return keyboardBuilder.build();
    }

    @Override
    public boolean canHandleUpdate(Update update) {
        if (!update.hasMessage()) {
            return false;
        }

        ClientAction clientAction = clientActionRepository.findByChatId(update.getMessage().getChatId());
        if (clientAction == null || !ENTER_PHONE_NUMBER_ACTION.equals(clientAction.getAction())) {
            return false;
        }
        return update.getMessage().hasContact();
    }

    @Override
    public void handleUpdate(AbsSender absSender, Update update) throws TelegramApiException {
        Long chatId = update.getMessage().getChatId();
        String phoneNumber = update.getMessage().getContact().getPhoneNumber();

        handlePhoneNumber(absSender, update, chatId, phoneNumber);
    }

    @Override
    public boolean canHandleAction(Update update, String action) {
        return update.hasMessage() && update.getMessage().hasText() && ENTER_PHONE_NUMBER_ACTION.equals(action);
    }

    @Override
    public void handleAction(AbsSender absSender, Update update, String action) throws TelegramApiException {
        Long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();

        handlePhoneNumber(absSender, update, chatId, text);
    }

    private void handlePhoneNumber(AbsSender absSender, Update update, Long chatId, String text)
            throws TelegramApiException {
        
        if (Button.ORDER_STEP_NEXT.getAlias().equals(text)) {
            executeNextCommand(absSender, update, chatId);
            return;
        }
        if (!PHONE_NUMBER_PATTERN.matcher(text).find()) {
            sendNotCorrectPhoneNumberMessage(absSender, chatId);
            return;
        }

        saveClientOrderState(chatId, text);
        executeNextCommand(absSender, update, chatId);
    }

    private void sendNotCorrectPhoneNumberMessage(AbsSender absSender, Long chatId) throws TelegramApiException {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("You entered the incorrect phone number, try again or press button.")
                .build();
        absSender.execute(message);
    }

    private void saveClientOrderState(Long chatId, String text) {
        ClientOrder clientOrder = clientOrderStateRepository.findByChatId(chatId);
        clientOrder.setPhoneNumber(text);
        clientOrderStateRepository.updateByChatId(chatId, clientOrder);
    }

    private void executeNextCommand(AbsSender absSender, Update update, Long chatId) throws TelegramApiException {
        clientCommandStateRepository.pushByChatId(chatId, getCommand());
        commandHandlerRegistry.find(Command.ENTER_CITY).executeCommand(absSender, update, chatId);
    }

}


import static java.util.stream.Collectors.toList;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.math.NumberUtils;
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;
import org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResult;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultArticle;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import ua.ivanzaitsev.bot.handlers.CommandHandler;
import ua.ivanzaitsev.bot.handlers.UpdateHandler;
import ua.ivanzaitsev.bot.handlers.commands.registries.CommandHandlerRegistry;
import ua.ivanzaitsev.bot.models.domain.Button;
import ua.ivanzaitsev.bot.models.domain.CartItem;
import ua.ivanzaitsev.bot.models.domain.Command;
import ua.ivanzaitsev.bot.models.domain.MessagePlaceholder;
import ua.ivanzaitsev.bot.models.entities.Category;
import ua.ivanzaitsev.bot.models.entities.Message;
import ua.ivanzaitsev.bot.models.entities.Product;
import ua.ivanzaitsev.bot.repositories.CartRepository;
import ua.ivanzaitsev.bot.repositories.CategoryRepository;
import ua.ivanzaitsev.bot.repositories.ProductRepository;
import ua.ivanzaitsev.bot.services.MessageService;

public class CatalogCommandHandler implements CommandHandler, UpdateHandler {

    private static final int PRODUCTS_QUANTITY_PER_PAGE = 50;
    private static final int MAX_QUANTITY_PER_PRODUCT = 50;
    private static final int MAX_PRODUCTS_QUANTITY_PER_CART = 50;

    private static final String PRODUCT_QUANTITY_CALLBACK = "catalog=product-quantity";
    private static final String MINUS_PRODUCT_CALLBACK = "catalog=minus-product_";
    private static final String PLUS_PRODUCT_CALLBACK = "catalog=plus-product_";
    private static final String OPEN_CATALOG_CALLBACK = "catalog=open-catalog";
    private static final String OPEN_CART_CALLBACK = "catalog=open-cart";

    private static final Set<String> CALLBACKS = Set.of(MINUS_PRODUCT_CALLBACK, PLUS_PRODUCT_CALLBACK,
            OPEN_CATALOG_CALLBACK, OPEN_CART_CALLBACK);

    private final CommandHandlerRegistry commandHandlerRegistry;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final MessageService messageService;

    public CatalogCommandHandler(
            CommandHandlerRegistry commandHandlerRegistry,
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            CartRepository cartRepository,
            MessageService messageService) {

        this.commandHandlerRegistry = commandHandlerRegistry;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.cartRepository = cartRepository;
        this.messageService = messageService;
    }

    @Override
    public Command getCommand() {
        return Command.CATALOG;
    }

    @Override
    public void executeCommand(AbsSender absSender, Update update, Long chatId) throws TelegramApiException {
        handleCatalogMessageUpdate(absSender, chatId);
    }

    @Override
    public boolean canHandleUpdate(Update update) {
        return isCatalogMessageUpdate(update) || isInlineQueryUpdate(update) || isCallbackQueryUpdate(update);
    }

    @Override
    public void handleUpdate(AbsSender absSender, Update update) throws TelegramApiException {
        if (isCatalogMessageUpdate(update)) {
            handleCatalogMessageUpdate(absSender, update.getMessage().getChatId());
        }
        if (isInlineQueryUpdate(update)) {
            handleInlineQueryUpdate(absSender, update);
        }
        if (isCallbackQueryUpdate(update)) {
            handleCallbackQueryUpdate(absSender, update);
        }
    }

    private boolean isCatalogMessageUpdate(Update update) {
        return update.hasMessage() &&
                update.getMessage().hasText() &&
                update.getMessage().getText().equals(Button.CATALOG.getAlias());
    }

    private boolean isInlineQueryUpdate(Update update) {
        return update.hasInlineQuery();
    }

    private boolean isCallbackQueryUpdate(Update update) {
        return update.hasCallbackQuery() && 
                CALLBACKS.stream().anyMatch(callback -> update.getCallbackQuery().getData().startsWith(callback));
    }

    private void handleCatalogMessageUpdate(AbsSender absSender, Long chatId) throws TelegramApiException {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Choose a category:")
                .replyMarkup(createCategoriesKeyboard())
                .build();
        absSender.execute(message);
    }

    private InlineKeyboardMarkup createCategoriesKeyboard() {
        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder keyboardBuilder = InlineKeyboardMarkup.builder();

        for (Category category : categoryRepository.findAll()) {
            String categoryName = category.getName();

            keyboardBuilder.keyboardRow(Arrays.asList(
                    InlineKeyboardButton.builder().text(categoryName).switchInlineQueryCurrentChat(categoryName).build()
                    ));
        }

        return keyboardBuilder.build();
    }

    private void handleInlineQueryUpdate(AbsSender absSender, Update update) throws TelegramApiException {
        InlineQuery inlineQuery = update.getInlineQuery();
        String query = inlineQuery.getQuery();

        int offset = NumberUtils.toInt(inlineQuery.getOffset(), 0);
        List<Product> products = productRepository.findAllByCategoryName(query, offset, PRODUCTS_QUANTITY_PER_PAGE);
        if (!products.isEmpty()) {
            int nextOffset = offset + PRODUCTS_QUANTITY_PER_PAGE;
            sendCatalogQueryAnswer(absSender, inlineQuery, products, nextOffset);
        }
    }

    private void sendCatalogQueryAnswer(AbsSender absSender, InlineQuery inlineQuery, List<Product> products,
            Integer nextOffset) throws TelegramApiException {

        Long chatId = inlineQuery.getFrom().getId();

        List<InlineQueryResult> queryResults = products.stream()
                .map(product -> buildProductArticle(chatId, product)).collect(toList());

        AnswerInlineQuery answerInlineQuery = AnswerInlineQuery.builder()
                .inlineQueryId(inlineQuery.getId())
                .results(queryResults)
                .nextOffset(Integer.toString(nextOffset))
                .cacheTime(1)
                .build();
        absSender.execute(answerInlineQuery);
    }

    private InlineQueryResultArticle buildProductArticle(Long chatId, Product product) {
        return InlineQueryResultArticle.builder()
                .id(product.getId().toString())
                .thumbnailUrl(product.getPhotoUrl())
                .thumbnailHeight(48)
                .thumbnailHeight(48)
                .title(product.getName())
                .description(product.getDescription())
                .replyMarkup(createProductKeyboard(chatId, product))
                .inputMessageContent(
                        InputTextMessageContent.builder()
                            .parseMode("HTML").messageText(createProductText(chatId, product)).build()
                        )
                .build();
    }

    private void handleCallbackQueryUpdate(AbsSender absSender, Update update) throws TelegramApiException {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Long chatId = callbackQuery.getFrom().getId();
        String inlineMessageId = callbackQuery.getInlineMessageId();
        String data = callbackQuery.getData();

        if (data.startsWith(MINUS_PRODUCT_CALLBACK)) {
            doMinusProduct(absSender, chatId, inlineMessageId, data);
        }
        if (data.startsWith(PLUS_PRODUCT_CALLBACK)) {
            doPlusProduct(absSender, chatId, inlineMessageId, data);
        }
        if (data.equals(OPEN_CATALOG_CALLBACK)) {
            commandHandlerRegistry.find(Command.CATALOG).executeCommand(absSender, update, chatId);
        }
        if (data.equals(OPEN_CART_CALLBACK)) {
            commandHandlerRegistry.find(Command.CART).executeCommand(absSender, update, chatId);
        }
    }

    private void doPlusProduct(AbsSender absSender, Long chatId, String inlineMessageId, String data)
            throws TelegramApiException {

        Integer productId = Integer.parseInt(data.split("_")[1]);
        CartItem cartItem = cartRepository.findCartItemByChatIdAndProductId(chatId, productId);
        Product product = cartItem != null ? cartItem.getProduct() : productRepository.findById(productId);

        if (product == null) {
            return;
        }

        if (cartItem != null) {
            if (cartItem.getQuantity() < MAX_QUANTITY_PER_PRODUCT) {
                cartItem.setQuantity(cartItem.getQuantity() + 1);
                cartRepository.updateCartItem(chatId, cartItem);
            }
        } else {
            if (cartRepository.findAllCartItemsByChatId(chatId).size() < MAX_PRODUCTS_QUANTITY_PER_CART) {
                cartRepository.saveCartItem(chatId, new CartItem(product, 1));
            }
        }

        editProductMessage(absSender, chatId, inlineMessageId, product);
    }

    private void doMinusProduct(AbsSender absSender, Long chatId, String inlineMessageId, String data)
            throws TelegramApiException {

        Integer productId = Integer.parseInt(data.split("_")[1]);
        CartItem cartItem = cartRepository.findCartItemByChatIdAndProductId(chatId, productId);
        Product product = cartItem != null ? cartItem.getProduct() : productRepository.findById(productId);

        if (product == null) {
            return;
        }

        if (cartItem != null) {
            if (cartItem.getQuantity() > 1) {
                cartItem.setQuantity(cartItem.getQuantity() - 1);
                cartRepository.updateCartItem(chatId, cartItem);
            } else {
                cartRepository.deleteCartItem(chatId, cartItem.getId());
            }
        }

        editProductMessage(absSender, chatId, inlineMessageId, product);
    }

    private void editProductMessage(AbsSender absSender, Long chatId, String inlineMessageId, Product product)
            throws TelegramApiException {

        EditMessageText message = EditMessageText.builder()
                .inlineMessageId(inlineMessageId)
                .text(createProductText(chatId, product))
                .replyMarkup(createProductKeyboard(chatId, product))
                .parseMode("HTML")
                .build();
        absSender.execute(message);
    }

    private String createProductText(Long chatId, Product product) {
        Message message = messageService.findByName("PRODUCT_MESSAGE");

        message.applyPlaceholder(MessagePlaceholder.of("%PRODUCT_PHOTO_URL%", product.getPhotoUrl()));
        message.applyPlaceholder(MessagePlaceholder.of("%PRODUCT_NAME%", product.getName()));
        message.applyPlaceholder(MessagePlaceholder.of("%PRODUCT_DESCRIPTION%", product.getDescription()));

        CartItem cartItem = cartRepository.findCartItemByChatIdAndProductId(chatId, product.getId());

        if (cartItem == null) {
            message.removeTextBetweenPlaceholder("%PRODUCT_PRICES%");
        } else {
            message.applyPlaceholder(MessagePlaceholder.of("%PRODUCT_PRICE%", product.getPrice()));
            message.applyPlaceholder(MessagePlaceholder.of("%PRODUCT_QUANTITY%", cartItem.getQuantity()));
            message.applyPlaceholder(
                    MessagePlaceholder.of("%PRODUCT_TOTAL_PRICE%", product.getPrice() * cartItem.getQuantity()));
        }

        return message.buildText();
    }

    private InlineKeyboardMarkup createProductKeyboard(Long chatId, Product product) {
        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder keyboardBuilder = InlineKeyboardMarkup.builder();

        CartItem cartItem = cartRepository.findCartItemByChatIdAndProductId(chatId, product.getId());

        if (cartItem != null) {
            keyboardBuilder.keyboardRow(Arrays.asList(
                    InlineKeyboardButton.builder()
                        .text("\u2796").callbackData(MINUS_PRODUCT_CALLBACK + product.getId()).build(),
                    InlineKeyboardButton.builder()
                        .text(cartItem.getQuantity() + " pcs.").callbackData(PRODUCT_QUANTITY_CALLBACK).build(),
                    InlineKeyboardButton.builder()
                        .text("\u2795").callbackData(PLUS_PRODUCT_CALLBACK + product.getId()).build()
                    ));
            keyboardBuilder.keyboardRow(Arrays.asList(
                    InlineKeyboardButton.builder()
                        .text(Button.CATALOG.getAlias()).callbackData(OPEN_CATALOG_CALLBACK).build(),
                    InlineKeyboardButton.builder()
                        .text(Button.CART.getAlias()).callbackData(OPEN_CART_CALLBACK).build()
                    ));
        } else {
            keyboardBuilder.keyboardRow(Arrays.asList(
                    InlineKeyboardButton.builder()
                        .text(String.format("\uD83D\uDCB5 Price: %d $ \uD83D\uDECD Add to cart", product.getPrice()))
                        .callbackData(PLUS_PRODUCT_CALLBACK + product.getId()).build()
                    ));
        }

        return keyboardBuilder.build();
    }

}


import java.util.Arrays;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import ua.ivanzaitsev.bot.handlers.ActionHandler;
import ua.ivanzaitsev.bot.handlers.CommandHandler;
import ua.ivanzaitsev.bot.handlers.commands.registries.CommandHandlerRegistry;
import ua.ivanzaitsev.bot.models.domain.Button;
import ua.ivanzaitsev.bot.models.domain.ClientAction;
import ua.ivanzaitsev.bot.models.domain.ClientOrder;
import ua.ivanzaitsev.bot.models.domain.Command;
import ua.ivanzaitsev.bot.repositories.ClientActionRepository;
import ua.ivanzaitsev.bot.repositories.ClientCommandStateRepository;
import ua.ivanzaitsev.bot.repositories.ClientOrderStateRepository;

public class OrderEnterNameCommandHandler implements CommandHandler, ActionHandler {

    private static final String ENTER_NAME_ACTION = "order=enter-client-name";

    private static final Pattern NAME_PATTERN = Pattern.compile("[a-zA-Z]");

    private final CommandHandlerRegistry commandHandlerRegistry;
    private final ClientActionRepository clientActionRepository;
    private final ClientCommandStateRepository clientCommandStateRepository;
    private final ClientOrderStateRepository clientOrderStateRepository;

    public OrderEnterNameCommandHandler(
            CommandHandlerRegistry commandHandlerRegistry,
            ClientActionRepository clientActionRepository,
            ClientCommandStateRepository clientCommandStateRepository,
            ClientOrderStateRepository clientOrderStateRepository) {

        this.commandHandlerRegistry = commandHandlerRegistry;
        this.clientActionRepository = clientActionRepository;
        this.clientCommandStateRepository = clientCommandStateRepository;
        this.clientOrderStateRepository = clientOrderStateRepository;
    }

    @Override
    public Command getCommand() {
        return Command.ENTER_NAME;
    }

    @Override
    public void executeCommand(AbsSender absSender, Update update, Long chatId) throws TelegramApiException {
        clientActionRepository.updateByChatId(chatId, new ClientAction(getCommand(), ENTER_NAME_ACTION));

        sendEnterNameMessage(absSender, chatId);
        sendCurrentNameMessage(absSender, chatId);
    }

    private void sendEnterNameMessage(AbsSender absSender, Long chatId) throws TelegramApiException {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Enter your name")
                .replyMarkup(buildReplyKeyboardMarkup(false))
                .build();
        absSender.execute(message);
    }

    private void sendCurrentNameMessage(AbsSender absSender, Long chatId) throws TelegramApiException {
        ClientOrder clientOrder = clientOrderStateRepository.findByChatId(chatId);
        if (StringUtils.isBlank(clientOrder.getClientName())) {
            return;
        }

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Current name: " + clientOrder.getClientName())
                .replyMarkup(buildReplyKeyboardMarkup(true))
                .build();
        absSender.execute(message);
    }

    private ReplyKeyboardMarkup buildReplyKeyboardMarkup(boolean skip) {
        ReplyKeyboardMarkup.ReplyKeyboardMarkupBuilder keyboardBuilder = ReplyKeyboardMarkup.builder();
        keyboardBuilder.resizeKeyboard(true);
        keyboardBuilder.selective(true);

        if (skip) {
            keyboardBuilder.keyboardRow(new KeyboardRow(Arrays.asList(
                    KeyboardButton.builder().text(Button.ORDER_STEP_NEXT.getAlias()).build()
                    )));
        }

        keyboardBuilder.keyboardRow(new KeyboardRow(Arrays.asList(
                KeyboardButton.builder().text(Button.ORDER_STEP_CANCEL.getAlias()).build()
                )));
        return keyboardBuilder.build();
    }

    @Override
    public boolean canHandleAction(Update update, String action) {
        return update.hasMessage() && update.getMessage().hasText() && ENTER_NAME_ACTION.equals(action);
    }

    @Override
    public void handleAction(AbsSender absSender, Update update, String action) throws TelegramApiException {
        Long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();

        if (Button.ORDER_STEP_NEXT.getAlias().equals(text)) {
            executeNextCommand(absSender, update, chatId);
            return;
        }
        if (!NAME_PATTERN.matcher(text).find()) {
            sendNotCorrectNameMessage(absSender, chatId);
            return;
        }

        saveClientOrderState(chatId, text);
        executeNextCommand(absSender, update, chatId);
    }

    private void sendNotCorrectNameMessage(AbsSender absSender, Long chatId) throws TelegramApiException {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("You entered the incorrect name, try again.")
                .build();
        absSender.execute(message);
    }

    private void saveClientOrderState(Long chatId, String text) {
        ClientOrder clientOrder = clientOrderStateRepository.findByChatId(chatId);
        clientOrder.setClientName(text);
        clientOrderStateRepository.updateByChatId(chatId, clientOrder);
    }

    private void executeNextCommand(AbsSender absSender, Update update, Long chatId) throws TelegramApiException {
        clientCommandStateRepository.pushByChatId(chatId, getCommand());
        commandHandlerRegistry.find(Command.ENTER_PHONE_NUMBER).executeCommand(absSender, update, chatId);
    }

}


import java.util.List;

import ua.ivanzaitsev.bot.handlers.CommandHandler;
import ua.ivanzaitsev.bot.models.domain.Command;

public interface CommandHandlerRegistry {

    void setCommandHandlers(List<CommandHandler> commandHandlers);

    CommandHandler find(Command command);

}


import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;

import ua.ivanzaitsev.bot.exceptions.HandlerNotFoundException;
import ua.ivanzaitsev.bot.handlers.CommandHandler;
import ua.ivanzaitsev.bot.models.domain.Command;

public class CommandHandlerRegistryDefault implements CommandHandlerRegistry {

    private Map<Command, CommandHandler> commandHandlers;

    @Override
    public void setCommandHandlers(List<CommandHandler> commandHandlers) {
        this.commandHandlers = commandHandlers.stream().collect(toMap(CommandHandler::getCommand, identity()));
    }

    @Override
    public CommandHandler find(Command command) {
        CommandHandler commandHandler = commandHandlers.get(command);
        if (commandHandler == null) {
            throw new HandlerNotFoundException("CommandHandler with name '" + command + "' not found");
        }
        return commandHandler;
    }

}


import ua.ivanzaitsev.bot.models.entities.Message;

public interface MessageRepository {

    Message findByName(String messageName);

}


import ua.ivanzaitsev.bot.models.domain.ClientOrder;

public interface ClientOrderStateRepository {

    ClientOrder findByChatId(Long chatId);

    void updateByChatId(Long chatId, ClientOrder clientOrder);

    void deleteByChatId(Long chatId);

}


import ua.ivanzaitsev.bot.models.domain.ClientAction;

public interface ClientActionRepository {

    ClientAction findByChatId(Long chatId);

    void updateByChatId(Long chatId, ClientAction clientAction);

    void deleteByChatId(Long chatId);

}


import ua.ivanzaitsev.bot.models.domain.Command;

public interface ClientCommandStateRepository {

    void pushByChatId(Long chatId, Command command);

    Command popByChatId(Long chatId);

    void deleteAllByChatId(Long chatId);

}


import java.util.List;

import ua.ivanzaitsev.bot.models.domain.CartItem;

public interface CartRepository {

    void saveCartItem(Long chatId, CartItem cartItem);

    void updateCartItem(Long chatId, CartItem cartItem);

    void deleteCartItem(Long chatId, Integer cartItemId);

    CartItem findCartItemByChatIdAndProductId(Long chatId, Integer productId);

    List<CartItem> findAllCartItemsByChatId(Long chatId);

    void deleteAllCartItemsByChatId(Long chatId);

    void updatePageNumberByChatId(Long chatId, Integer pageNumber);

    Integer findPageNumberByChatId(Long chatId);

}


import ua.ivanzaitsev.bot.models.entities.Client;

public interface ClientRepository {

    Client findByChatId(Long chatId);

    void save(Client client);

    void update(Client client);

}


import java.util.List;

import ua.ivanzaitsev.bot.models.entities.Product;

public interface ProductRepository {

    Product findById(Integer productId);

    List<Product> findAllByCategoryName(String categoryName, int offset, int size);

}


import ua.ivanzaitsev.bot.models.entities.Order;

public interface OrderRepository {

    void save(Order order);

}


import java.util.List;

import ua.ivanzaitsev.bot.models.entities.Category;

public interface CategoryRepository {

    List<Category> findAll();

}


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.SerializationUtils;

import ua.ivanzaitsev.bot.models.domain.ClientOrder;
import ua.ivanzaitsev.bot.repositories.ClientOrderStateRepository;

public class ClientOrderStateRepositoryDefault implements ClientOrderStateRepository {

    private final Map<Long, ClientOrder> clientOrders = new ConcurrentHashMap<>();

    @Override
    public ClientOrder findByChatId(Long chatId) {
        ClientOrder clientOrder = clientOrders.get(chatId);
        return SerializationUtils.clone(clientOrder);
    }

    @Override
    public void updateByChatId(Long chatId, ClientOrder userOrder) {
        clientOrders.put(chatId, SerializationUtils.clone(userOrder));
    }

    @Override
    public void deleteByChatId(Long chatId) {
        clientOrders.remove(chatId);
    }

}


import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ua.ivanzaitsev.bot.models.domain.Command;
import ua.ivanzaitsev.bot.repositories.ClientCommandStateRepository;

public class ClientCommandStateRepositoryDefault implements ClientCommandStateRepository {

    private final Map<Long, List<Command>> userCommands = new ConcurrentHashMap<>();

    @Override
    public void pushByChatId(Long chatId, Command command) {
        List<Command> commands = userCommands.computeIfAbsent(chatId, value -> new LinkedList<>());
        commands.add(command);
    }

    @Override
    public Command popByChatId(Long chatId) {
        List<Command> commands = userCommands.computeIfAbsent(chatId, value -> new LinkedList<>());
        if (commands.isEmpty()) {
            return null;
        }
        return commands.remove(commands.size() - 1);
    }

    @Override
    public void deleteAllByChatId(Long chatId) {
        userCommands.remove(chatId);
    }

}


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.SerializationUtils;

import ua.ivanzaitsev.bot.models.domain.ClientAction;
import ua.ivanzaitsev.bot.repositories.ClientActionRepository;

public class ClientActionRepositoryDefault implements ClientActionRepository {

    private final Map<Long, ClientAction> clientsAction = new ConcurrentHashMap<>();

    @Override
    public ClientAction findByChatId(Long chatId) {
        ClientAction clientAction = clientsAction.get(chatId);
        return SerializationUtils.clone(clientAction);
    }

    @Override
    public void updateByChatId(Long chatId, ClientAction clientAction) {
        clientsAction.put(chatId, SerializationUtils.clone(clientAction));
    }

    @Override
    public void deleteByChatId(Long chatId) {
        clientsAction.remove(chatId);
    }

}


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.SerializationUtils;

import ua.ivanzaitsev.bot.models.domain.CartItem;
import ua.ivanzaitsev.bot.repositories.CartRepository;

public class CartRepositoryDefault implements CartRepository {

    private final AtomicInteger lastCartItemId = new AtomicInteger();
    private final Map<Long, List<CartItem>> cartItems = new ConcurrentHashMap<>();

    private final Map<Long, Integer> cartPageNumbers = new ConcurrentHashMap<>();

    @Override
    public void saveCartItem(Long chatId, CartItem cartItem) {
        cartItems.computeIfAbsent(chatId, orderItems -> new ArrayList<>());

        cartItem.setId(lastCartItemId.incrementAndGet());
        cartItems.get(chatId).add(SerializationUtils.clone(cartItem));
    }

    @Override
    public void updateCartItem(Long chatId, CartItem cartItem) {
        cartItems.computeIfAbsent(chatId, value -> new ArrayList<>());

        List<CartItem> receivedCartItems = cartItems.get(chatId);
        IntStream.range(0, receivedCartItems.size())
                .filter(i -> cartItem.getId().equals(receivedCartItems.get(i).getId()))
                .findFirst()
                .ifPresent(i -> receivedCartItems.set(i, SerializationUtils.clone(cartItem)));
    }

    @Override
    public void deleteCartItem(Long chatId, Integer cartItemId) {
        cartItems.computeIfAbsent(chatId, value -> new ArrayList<>());

        List<CartItem> receivedCartItems = cartItems.get(chatId);
        receivedCartItems.stream()
                .filter(cartItem -> cartItem.getId().equals(cartItemId))
                .findFirst()
                .ifPresent(receivedCartItems::remove);
    }

    @Override
    public CartItem findCartItemByChatIdAndProductId(Long chatId, Integer productId) {
        cartItems.computeIfAbsent(chatId, value -> new ArrayList<>());

        return cartItems.get(chatId).stream()
                .filter(cartItem -> cartItem.getProduct().getId().equals(productId))
                .findFirst()
                .map(SerializationUtils::clone)
                .orElse(null);
    }

    @Override
    public List<CartItem> findAllCartItemsByChatId(Long chatId) {
        cartItems.computeIfAbsent(chatId, value -> new ArrayList<>());

        return cartItems.get(chatId).stream()
                .map(SerializationUtils::clone)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteAllCartItemsByChatId(Long chatId) {
        cartItems.remove(chatId);
    }

    @Override
    public Integer findPageNumberByChatId(Long chatId) {
        return cartPageNumbers.getOrDefault(chatId, 0);
    }

    @Override
    public void updatePageNumberByChatId(Long chatId, Integer pageNumber) {
        cartPageNumbers.put(chatId, pageNumber);
    }

}


import static ua.ivanzaitsev.bot.repositories.hibernate.HibernateTransactionFactory.inTransaction;

import ua.ivanzaitsev.bot.models.entities.Message;
import ua.ivanzaitsev.bot.repositories.MessageRepository;

public class MessageRepositoryDefault implements MessageRepository {

    @Override
    public Message findByName(String messageName) {
        String query = "from Message where name = :name";

        return inTransaction(session ->
                session.createQuery(query, Message.class)
                        .setParameter("name", messageName)
                        .setMaxResults(1)
                        .uniqueResult()
        );
    }

}


import static ua.ivanzaitsev.bot.repositories.hibernate.HibernateTransactionFactory.inTransaction;

import java.util.List;

import ua.ivanzaitsev.bot.models.entities.Category;
import ua.ivanzaitsev.bot.repositories.CategoryRepository;

public class CategoryRepositoryDefault implements CategoryRepository {

    @Override
    public List<Category> findAll() {
        String query = "from Category";

        return inTransaction(session -> session.createQuery(query, Category.class).getResultList());
    }

}


import static ua.ivanzaitsev.bot.repositories.hibernate.HibernateTransactionFactory.inTransaction;
import static ua.ivanzaitsev.bot.repositories.hibernate.HibernateTransactionFactory.inTransactionVoid;

import ua.ivanzaitsev.bot.models.entities.Client;
import ua.ivanzaitsev.bot.repositories.ClientRepository;

public class ClientRepositoryDefault implements ClientRepository {

    @Override
    public Client findByChatId(Long chatId) {
        String query = "from Client where chatId = :chatId";

        return inTransaction(session ->
                session.createQuery(query, Client.class)
                        .setParameter("chatId", chatId)
                        .setMaxResults(1)
                        .uniqueResult()
        );
    }

    @Override
    public void save(Client client) {
        inTransactionVoid(session -> session.persist(client));
    }

    @Override
    public void update(Client client) {
        inTransactionVoid(session -> session.merge(client));
    }

}


import static ua.ivanzaitsev.bot.repositories.hibernate.HibernateTransactionFactory.inTransaction;

import java.util.List;

import ua.ivanzaitsev.bot.models.entities.Product;
import ua.ivanzaitsev.bot.repositories.ProductRepository;

public class ProductRepositoryDefault implements ProductRepository {

    @Override
    public Product findById(Integer productId) {
        return inTransaction(session -> session.get(Product.class, productId));
    }

    @Override
    public List<Product> findAllByCategoryName(String categoryName, int offset, int size) {
        String query = "from Product where category.name = :categoryName";

        return inTransaction(session ->
                session.createQuery(query, Product.class)
                        .setParameter("categoryName", categoryName)
                        .setFirstResult(offset)
                        .setMaxResults(size)
                        .getResultList()
        );
    }

}


import static ua.ivanzaitsev.bot.repositories.hibernate.HibernateTransactionFactory.inTransactionVoid;

import ua.ivanzaitsev.bot.models.entities.Order;
import ua.ivanzaitsev.bot.repositories.OrderRepository;

public class OrderRepositoryDefault implements OrderRepository {

    @Override
    public void save(Order order) {
        inTransactionVoid(session -> session.persist(order));
    }

}


import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;

import ua.ivanzaitsev.bot.models.entities.Category;
import ua.ivanzaitsev.bot.models.entities.Client;
import ua.ivanzaitsev.bot.models.entities.Message;
import ua.ivanzaitsev.bot.models.entities.Order;
import ua.ivanzaitsev.bot.models.entities.OrderItem;
import ua.ivanzaitsev.bot.models.entities.OrderStatus;
import ua.ivanzaitsev.bot.models.entities.Product;

public final class HibernateSessionFactory {

    private static final SessionFactory SESSION_FACTORY = buildSessionFactory();

    private HibernateSessionFactory() {
    }

    private static SessionFactory buildSessionFactory() {
        Configuration configuration = new Configuration();
        configuration.configure();

        new EnvironmentPropertiesPopulator().populate(configuration.getProperties());
        addAnnotatedClasses(configuration);

        StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder();
        builder.applySettings(configuration.getProperties());
        return configuration.buildSessionFactory(builder.build());
    }

    private static void addAnnotatedClasses(Configuration configuration) {
        configuration.addAnnotatedClass(Category.class);
        configuration.addAnnotatedClass(Client.class);
        configuration.addAnnotatedClass(Order.class);
        configuration.addAnnotatedClass(OrderItem.class);
        configuration.addAnnotatedClass(OrderStatus.class);
        configuration.addAnnotatedClass(Product.class);
        configuration.addAnnotatedClass(Message.class);
    }

    public static SessionFactory getSessionFactory() {
        return SESSION_FACTORY;
    }

}


import java.util.function.Function;

import org.hibernate.Session;

@FunctionalInterface
public interface TransactionFunction<T> extends Function<Session, T> {

}


import java.util.function.Consumer;

import org.hibernate.Session;

@FunctionalInterface
public interface TransactionVoidFunction extends Consumer<Session> {

}


import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.resource.transaction.spi.TransactionStatus;

public final class HibernateTransactionFactory {

    private static final SessionFactory SESSION_FACTORY = HibernateSessionFactory.getSessionFactory();

    private HibernateTransactionFactory() {
    }

    public static void inTransactionVoid(TransactionVoidFunction function) {
        inTransaction(session -> {
            function.accept(session);
            return null;
        });
    }

    public static <T> T inTransaction(TransactionFunction<T> function) {
        T result;

        Session session = SESSION_FACTORY.getCurrentSession();
        Transaction transaction = session.getTransaction();

        boolean isTransactionNotExists = TransactionStatus.NOT_ACTIVE.equals(transaction.getStatus());
        if (isTransactionNotExists) {
            transaction = session.beginTransaction();
        }

        try {
            result = function.apply(session);
            if (isTransactionNotExists) {
                transaction.commit();
            }
        } catch (Exception e) {
            if (isTransactionNotExists) {
                transaction.rollback();
            }
            throw e;
        } finally {
            if (isTransactionNotExists) {
                session.close();
            }
        }

        return result;
    }

}


import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class EnvironmentPropertiesPopulator {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{(.*?)}");

    public void populate(Properties properties) {
        properties.forEach(((propertyKey, propertyValue) -> {
            Matcher matcher = VARIABLE_PATTERN.matcher(String.valueOf(propertyValue));
            if (matcher.find()) {
                String environmentVariableKey = matcher.group(1);
                String environmentVariableValue = System.getenv(environmentVariableKey);

                if (environmentVariableValue == null) {
                    throw new IllegalStateException("Failed to get environment variable = " +
                            environmentVariableKey + " for property " + propertyKey);
                }

                properties.put(propertyKey, environmentVariableValue);
            }
        }));
    }

}


public enum Command {

    START,
    CATALOG,
    CART,
    ENTER_NAME,
    ENTER_PHONE_NUMBER,
    ENTER_CITY,
    ENTER_ADDRESS,
    ORDER_STEP_CANCEL,
    ORDER_STEP_PREVIOUS,
    ORDER_CONFIRM;

}


import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import ua.ivanzaitsev.bot.models.entities.Product;

public class CartItem implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer id;
    private Product product;
    private int quantity;

    public CartItem() {
    }

    public CartItem(Product product, Integer quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public Long getTotalPrice() {
        return quantity * product.getPrice();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CartItem cartItem = (CartItem) o;
        return Objects.equals(id, cartItem.id) && 
                Objects.equals(product, cartItem.product) && 
                Objects.equals(quantity, cartItem.quantity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, product, quantity);
    }

    @Override
    public String toString() {
        return "CartItem [id=" + id +
                ", product=" + product +
                ", quantity=" + quantity + "]";
    }

}


import java.util.Objects;

public class MessagePlaceholder {

    private final String placeholder;
    private final Object replacement;

    private MessagePlaceholder(String placeholder, Object replacement) {
        this.placeholder = placeholder;
        this.replacement = replacement;
    }

    public static MessagePlaceholder of(String placeholder, Object replacement) {
        return new MessagePlaceholder(placeholder, replacement);
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public Object getReplacement() {
        return replacement;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MessagePlaceholder that = (MessagePlaceholder) o;
        return Objects.equals(placeholder, that.placeholder) &&
                Objects.equals(replacement, that.replacement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(placeholder, replacement);
    }

    @Override
    public String toString() {
        return "MessagePlaceholder [placeholder=" + placeholder +
                ", replacement=" + replacement + "]";
    }

}


import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

public class ClientAction implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Command command;
    private final String action;
    private final LocalDateTime createdTime = LocalDateTime.now();

    public ClientAction(Command command, String action) {
        this.command = command;
        this.action = action;
    }

    public Command getCommand() {
        return command;
    }

    public String getAction() {
        return action;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ClientAction that = (ClientAction) o;
        return Objects.equals(command, that.command) &&
                Objects.equals(action, that.action) &&
                Objects.equals(createdTime, that.createdTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(command, action, createdTime);
    }

    @Override
    public String toString() {
        return "ClientAction [command=" + command +
                ", action=" + action +
                ", createdTime=" + createdTime + "]";
    }

}


import static org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton.builder;

import java.util.Arrays;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

public enum Button {

    START("/start"),
    CATALOG("\uD83D\uDCE6 Catalog"),
    CART("\uD83D\uDECD Cart"),
    SEND_PHONE_NUMBER("\uD83D\uDCF1 Send Phone Number"),
    ORDER_STEP_NEXT("\u2714\uFE0F Correct"),
    ORDER_STEP_PREVIOUS("\u25C0 Back"),
    ORDER_STEP_CANCEL("\u274C Cancel order"),
    ORDER_CONFIRM("\u2705 Confirm");

    private final String alias;

    Button(String alias) {
        this.alias = alias;
    }

    public String getAlias() {
        return alias;
    }

    public static ReplyKeyboardMarkup createGeneralMenuKeyboard() {
        ReplyKeyboardMarkup.ReplyKeyboardMarkupBuilder keyboardBuilder = ReplyKeyboardMarkup.builder();
        keyboardBuilder.resizeKeyboard(true);
        keyboardBuilder.selective(true);

        keyboardBuilder.keyboardRow(new KeyboardRow(Arrays.asList(
                builder().text(CATALOG.getAlias()).build(),
                builder().text(CART.getAlias()).build()
                )));

        return keyboardBuilder.build();
    }

}


import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class ClientOrder implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<CartItem> cartItems;
    private String clientName;
    private String phoneNumber;
    private String city;
    private String address;

    public long calculateTotalPrice() {
        long totalPrice = 0;
        for (CartItem cartItem : cartItems) {
            totalPrice += cartItem.getTotalPrice();
        }
        return totalPrice;
    }

    public List<CartItem> getCartItems() {
        return cartItems;
    }

    public void setCartItems(List<CartItem> cartItems) {
        this.cartItems = cartItems;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cartItems, clientName, phoneNumber, city, address);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ClientOrder other = (ClientOrder) obj;
        return Objects.equals(cartItems, other.cartItems) &&
                Objects.equals(clientName, other.clientName) &&
                Objects.equals(phoneNumber, other.phoneNumber) &&
                Objects.equals(city, other.city) &&
                Objects.equals(address, other.address);
    }

    @Override
    public String toString() {
        return "ClientOrder [cartItems=" + cartItems +
                ", clientName=" + clientName +
                ", phoneNumber=" + phoneNumber +
                ", city=" + city +
                ", address=" + address + "]";
    }

}


import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "categories")
public class Category implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "categories_seq")
    @SequenceGenerator(name = "categories_seq", sequenceName = "categories_id_seq", allocationSize = 1)
    private Integer id;

    @Column(nullable = false)
    private String name;

    public Category() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Category category = (Category) o;
        return Objects.equals(id, category.id) &&
                Objects.equals(name, category.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public String toString() {
        return "Category [id=" + id +
                ", name=" + name + "]";
    }

}


import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class Order implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "orders_seq")
    @SequenceGenerator(name = "orders_seq", sequenceName = "orders_id_seq", allocationSize = 1)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(nullable = false)
    private Long amount;

    @OneToMany(mappedBy = "order", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<OrderItem> items = new ArrayList<>();

    public Order() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        for (OrderItem item : items) {
            item.setOrder(this);
            this.items.add(item);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Order order = (Order) o;
        return Objects.equals(id, order.id) && 
                Objects.equals(client, order.client) && 
                Objects.equals(createdDate, order.createdDate) && 
                status == order.status && 
                Objects.equals(amount, order.amount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, client, createdDate, status, amount);
    }

    @Override
    public String toString() {
        return "Order [id=" + id + 
                ", client=" + client + 
                ", createdDate=" + createdDate + 
                ", status=" + status + 
                ", amount=" + amount + "]";
    }

}


import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "products")
public class Product implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "products_seq")
    @SequenceGenerator(name = "products_seq", sequenceName = "products_id_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "photo_url", nullable = false)
    private String photoUrl;

    @Column(nullable = false)
    private String name;

    @Column(length = 2550, nullable = false)
    private String description;

    @Column(nullable = false)
    private Long price;

    public Product() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getPrice() {
        return price;
    }

    public void setPrice(Long price) {
        this.price = price;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Product product = (Product) o;
        return Objects.equals(id, product.id) && 
                Objects.equals(category, product.category) && 
                Objects.equals(photoUrl, product.photoUrl) && 
                Objects.equals(name, product.name) && 
                Objects.equals(description, product.description) && 
                Objects.equals(price, product.price);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, category, photoUrl, name, description, price);
    }

    @Override
    public String toString() {
        return "Product [id=" + id + 
                ", category=" + category + 
                ", photoUrl=" + photoUrl + 
                ", name=" + name + 
                ", description=" + description + 
                ", price=" + price + "]";
    }

}


import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "clients")
public class Client implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "clients_seq")
    @SequenceGenerator(name = "clients_seq", sequenceName = "clients_id_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "chat_id", unique = true, nullable = false)
    private Long chatId;

    @Column
    private String name;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column
    private String city;

    @Column
    private String address;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    public Client() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Client client = (Client) o;
        return active == client.active && 
                Objects.equals(id, client.id) && 
                Objects.equals(chatId, client.chatId) && 
                Objects.equals(name, client.name) && 
                Objects.equals(phoneNumber, client.phoneNumber) && 
                Objects.equals(city, client.city) && 
                Objects.equals(address, client.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, chatId, name, phoneNumber, city, address, active);
    }

    @Override
    public String toString() {
        return "Client [id=" + id + 
                ", chatId=" + chatId + 
                ", name=" + name + 
                ", phoneNumber=" + phoneNumber + 
                ", city=" + city + 
                ", address=" + address + 
                ", active=" + active + "]";
    }

}


import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import ua.ivanzaitsev.bot.models.domain.MessagePlaceholder;

@Entity
@Table(name = "messages")
public class Message implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "messages_seq")
    @SequenceGenerator(name = "messages_seq", sequenceName = "messages_id_seq", allocationSize = 1)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(length = 4096, nullable = false)
    private String text;

    public Message() {
    }

    public void applyPlaceholder(MessagePlaceholder placeholder) {
        text = text.replace(placeholder.getPlaceholder(), placeholder.getReplacement().toString());
    }

    public void removeTextBetweenPlaceholder(String placeholderName) {
        text = text.replaceAll(placeholderName + "(?s).*" + placeholderName, "");
    }

    public void removeAllPlaceholders() {
        text = text.replaceAll("%.*%", "");
    }

    public String buildText() {
        removeAllPlaceholders();
        return text;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Message message = (Message) o;
        return Objects.equals(id, message.id) && 
                Objects.equals(name, message.name) && 
                Objects.equals(description, message.description) && 
                Objects.equals(text, message.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, text);
    }

    @Override
    public String toString() {
        return "Message [id=" + id + 
                ", name=" + name + 
                ", description=" + description + 
                ", text=" + text + "]";
    }

}


import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import ua.ivanzaitsev.bot.models.domain.CartItem;

@Entity
@Table(name = "orders_items")
public class OrderItem implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "orders_items_seq")
    @SequenceGenerator(name = "orders_items_seq", sequenceName = "orders_items_id_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "product_price", nullable = false)
    private Long productPrice;

    public OrderItem() {
    }

    public static OrderItem from(CartItem cartItem) {
        OrderItem orderItem = new OrderItem();
        orderItem.setProduct(cartItem.getProduct());
        orderItem.setQuantity(cartItem.getQuantity());
        orderItem.setProductName(cartItem.getProduct().getName());
        orderItem.setProductPrice(cartItem.getProduct().getPrice());
        return orderItem;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Long getProductPrice() {
        return productPrice;
    }

    public void setProductPrice(Long productPrice) {
        this.productPrice = productPrice;
    }

    public Long getTotalPrice() {
        return quantity * productPrice;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OrderItem orderItem = (OrderItem) o;
        return Objects.equals(id, orderItem.id) && 
                Objects.equals(order, orderItem.order) && 
                Objects.equals(product, orderItem.product) && 
                Objects.equals(quantity, orderItem.quantity) && 
                Objects.equals(productName, orderItem.productName) && 
                Objects.equals(productPrice, orderItem.productPrice);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, order, product, quantity, productName, productPrice);
    }

    @Override
    public String toString() {
        return "OrderItem [id=" + id + 
                ", order=" + order + 
                ", product=" + product + 
                ", quantity=" + quantity + 
                ", productName=" + productName + 
                ", productPrice=" + productPrice + "]";
    }

}


import java.io.Serial;
import java.io.Serializable;

public enum OrderStatus implements Serializable {

    WAITING("Waiting"),
    PROCESSED("Processed"),
    COMPLETED("Completed"),
    CANCELED("Canceled");
    
    @Serial
    private static final long serialVersionUID = 1L;

    private final String value;

    OrderStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}


import ua.ivanzaitsev.bot.models.entities.Message;

public interface MessageService {

    Message findByName(String messageName);

}


import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import ua.ivanzaitsev.bot.models.entities.Order;

public interface NotificationService {

    void notifyAdminChatAboutNewOrder(AbsSender absSender, Order order) throws TelegramApiException;

}


import java.util.List;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import ua.ivanzaitsev.bot.core.ConfigReader;
import ua.ivanzaitsev.bot.models.entities.Client;
import ua.ivanzaitsev.bot.models.entities.Order;
import ua.ivanzaitsev.bot.models.entities.OrderItem;
import ua.ivanzaitsev.bot.services.NotificationService;

public class NotificationServiceDefault implements NotificationService {

    private final String adminPanelBaseUrl;
    private final Long telegramAdminChatId;

    public NotificationServiceDefault(ConfigReader configReader) {
        this.adminPanelBaseUrl = configReader.get("admin-panel.base-url");
        this.telegramAdminChatId = Long.parseLong(configReader.get("telegram.admin.chat-id"));
    }

    @Override
    public void notifyAdminChatAboutNewOrder(AbsSender absSender, Order order) throws TelegramApiException {
        sendOrderAndClientInformationMessage(absSender, order);
        sendOrderItemsInformationMessage(absSender, order);
    }
    private void sendOrderAndClientInformationMessage(AbsSender absSender, Order order) throws TelegramApiException {
        SendMessage message = SendMessage.builder()
                .chatId(telegramAdminChatId)
                .text(createOrderAndClientInformation(order))
                .parseMode("HTML")
                .build();
        absSender.execute(message);
    }

    private void sendOrderItemsInformationMessage(AbsSender absSender, Order order) throws TelegramApiException {
        SendMessage message = SendMessage.builder()
                .chatId(telegramAdminChatId)
                .text(createOrderItemsInformation(order))
                .parseMode("HTML")
                .build();
        absSender.execute(message);
    }

    private String createOrderAndClientInformation(Order order) {
        return "#order_" + order.getId() + "\n" +
                "<b>Order url</b>:\n" + buildOrderUrl(order.getId()) + "\n\n" +
                "<b>Order information</b>:\n" + buildOrderInformation(order) + "\n\n" +
                "<b>Client information</b>:\n" + buildClientInformation(order.getClient());
    }

    private String buildOrderUrl(Integer orderId) {
        return adminPanelBaseUrl + "/orders/edit/" + orderId;
    }

    private String buildOrderInformation(Order order) {
        return "-Amount: " + order.getAmount() + " $";
    }

    private String buildClientInformation(Client client) {
        return "-Name: " + client.getName() + "\n" +
                "-Phone number: " + client.getPhoneNumber() + "\n" +
                "-City: " + client.getCity() + "\n" +
                "-Address: " + client.getAddress() + "\n" +
                "<a href=\"tg://user?id=" + client.getChatId() + "\">Open profile</a>";
    }

    private String createOrderItemsInformation(Order order) {
        return "#order_" + order.getId() + "\n" +
                "<b>Order items</b>:\n" + buildOrderItemsInformation(order.getItems());
    }

    private String buildOrderItemsInformation(List<OrderItem> orderItems) {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < orderItems.size(); i++) {
            OrderItem orderItem = orderItems.get(i);

            result.append(i + 1).append(") ").append(orderItem.getProductName()).append(" — ")
                    .append(orderItem.getQuantity()).append(" pcs. = ")
                    .append(orderItem.getProductPrice() * orderItem.getQuantity()).append(" $\n");
        }

        return result.toString();
    }

}


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.SerializationUtils;

import ua.ivanzaitsev.bot.models.entities.Message;
import ua.ivanzaitsev.bot.repositories.MessageRepository;
import ua.ivanzaitsev.bot.repositories.database.MessageRepositoryDefault;
import ua.ivanzaitsev.bot.services.MessageService;

public class MessageServiceDefault implements MessageService {

    private MessageRepository messageRepository = new MessageRepositoryDefault();

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private final Map<String, Message> cachedMessages = new HashMap<>();

    public MessageServiceDefault() {
        startCacheClearTask();
    }

    private void startCacheClearTask() {
        executorService.scheduleAtFixedRate(cachedMessages::clear, 20, 20, TimeUnit.MINUTES);
    }

    public void setRepository(MessageRepository repository) {
        this.messageRepository = repository;
    }

    @Override
    public Message findByName(String messageName) {
        if (messageName == null) {
            throw new IllegalArgumentException("MessageName should not be NULL");
        }

        Message message = cachedMessages.get(messageName);
        if (message == null) {
            message = messageRepository.findByName(messageName);
            cachedMessages.put(messageName, message);
        }

        return SerializationUtils.clone(message);
    }

}

