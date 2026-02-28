package com.finance.tracker.service.impl;

import com.finance.tracker.dto.request.TransactionRequest;
import com.finance.tracker.dto.response.TransactionResponse;
import com.finance.tracker.entity.Account;
import com.finance.tracker.entity.Category;
import com.finance.tracker.entity.Tag;
import com.finance.tracker.entity.Transaction;
import com.finance.tracker.entity.User;
import com.finance.tracker.mapper.TransactionMapper;
import com.finance.tracker.repository.AccountRepository;
import com.finance.tracker.repository.CategoryRepository;
import com.finance.tracker.repository.TagRepository;
import com.finance.tracker.repository.TransactionRepository;
import com.finance.tracker.repository.UserRepository;
import com.finance.tracker.service.TransactionService;
import jakarta.persistence.EntityNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;

    public TransactionServiceImpl(
        TransactionRepository transactionRepository,
        AccountRepository accountRepository,
        CategoryRepository categoryRepository,
        TagRepository tagRepository,
        UserRepository userRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getById(Long id) {
        Transaction transaction = transactionRepository.findWithRelationsById(id)
            .orElseThrow(() -> new EntityNotFoundException("Transaction not found: " + id));
        return TransactionMapper.toResponse(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> getAll() {
        return transactionRepository.findAllWithRelations().stream()
            .map(TransactionMapper::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> getByCategory(String category, boolean useEntityGraph) {
        List<Transaction> transactions = useEntityGraph
            ? transactionRepository.findWithRelationsByCategoryNameIgnoreCase(category)
            : transactionRepository.findByCategoryNameIgnoreCase(category);
        return transactions.stream().map(TransactionMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public TransactionResponse create(TransactionRequest request) {
        Transaction transaction = buildTransaction(request);
        return TransactionMapper.toResponse(transactionRepository.save(transaction));
    }

    @Override
    @Transactional
    public TransactionResponse update(Long id, TransactionRequest request) {
        Transaction existing = transactionRepository.findWithRelationsById(id)
            .orElseThrow(() -> new EntityNotFoundException("Transaction not found: " + id));
        User owner = resolveOwner(request.getOwnerEmail(), request.getOwnerName());
        Account account = resolveAccount(owner, request.getAccountName());
        Category category = resolveCategory(request.getCategoryName());
        Set<Tag> tags = resolveTags(request.getTagNames());
        TransactionMapper.updateEntity(existing, request, account, category, tags);
        return TransactionMapper.toResponse(transactionRepository.save(existing));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        transactionRepository.deleteById(id);
    }

    @Override
    public void demonstratePartialSave(TransactionRequest request) {
        User owner = createIsolatedOwner(request.getOwnerEmail(), request.getOwnerName(), "-no-tx");
        Account account = createIsolatedAccount(owner, request.getAccountName(), "-no-tx");
        Category category = createIsolatedCategory(request.getCategoryName(), "-no-tx");
        Set<Tag> tags = createIsolatedTags(request.getTagNames(), "-no-tx");
        transactionRepository.save(TransactionMapper.toEntity(request, account, category, tags));
        throw new IllegalStateException("Demo exception without @Transactional");
    }

    @Override
    @Transactional
    public void demonstrateRolledBackSave(TransactionRequest request) {
        User owner = createIsolatedOwner(request.getOwnerEmail(), request.getOwnerName(), "-with-tx");
        Account account = createIsolatedAccount(owner, request.getAccountName(), "-with-tx");
        Category category = createIsolatedCategory(request.getCategoryName(), "-with-tx");
        Set<Tag> tags = createIsolatedTags(request.getTagNames(), "-with-tx");
        transactionRepository.save(TransactionMapper.toEntity(request, account, category, tags));
        throw new IllegalStateException("Demo exception with @Transactional");
    }

    private Transaction buildTransaction(TransactionRequest request) {
        User owner = resolveOwner(request.getOwnerEmail(), request.getOwnerName());
        Account account = resolveAccount(owner, request.getAccountName());
        Category category = resolveCategory(request.getCategoryName());
        Set<Tag> tags = resolveTags(request.getTagNames());
        return TransactionMapper.toEntity(request, account, category, tags);
    }

    private User resolveOwner(String email, String fullName) {
        return userRepository.findByEmailIgnoreCase(email)
            .orElseGet(() -> {
                User user = new User();
                user.setEmail(email);
                user.setFullName(fullName);
                return userRepository.save(user);
            });
    }

    private Account resolveAccount(User owner, String accountName) {
        return accountRepository.findByNameIgnoreCase(accountName)
            .orElseGet(() -> {
                Account account = new Account();
                account.setName(accountName);
                account.setOwner(owner);
                return accountRepository.save(account);
            });
    }

    private Category resolveCategory(String categoryName) {
        return categoryRepository.findByNameIgnoreCase(categoryName)
            .orElseGet(() -> {
                Category category = new Category();
                category.setName(categoryName);
                return categoryRepository.save(category);
            });
    }

    private Set<Tag> resolveTags(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return new HashSet<>();
        }
        return tagNames.stream()
            .filter(tag -> tag != null && !tag.isBlank())
            .map(this::resolveTag)
            .collect(Collectors.toCollection(HashSet::new));
    }

    private Tag resolveTag(String tagName) {
        return tagRepository.findByNameIgnoreCase(tagName)
            .orElseGet(() -> {
                Tag tag = new Tag();
                tag.setName(tagName);
                return tagRepository.save(tag);
            });
    }

    private User createIsolatedOwner(String email, String fullName, String suffix) {
        User owner = new User();
        owner.setEmail(email + suffix);
        owner.setFullName(fullName + suffix);
        return userRepository.save(owner);
    }

    private Account createIsolatedAccount(User owner, String accountName, String suffix) {
        Account account = new Account();
        account.setName(accountName + suffix);
        account.setOwner(owner);
        return accountRepository.save(account);
    }

    private Category createIsolatedCategory(String categoryName, String suffix) {
        Category category = new Category();
        category.setName(categoryName + suffix);
        return categoryRepository.save(category);
    }

    private Set<Tag> createIsolatedTags(List<String> tagNames, String suffix) {
        if (tagNames == null || tagNames.isEmpty()) {
            return new HashSet<>();
        }
        return tagNames.stream()
            .filter(tag -> tag != null && !tag.isBlank())
            .map(tagName -> {
                Tag tag = new Tag();
                tag.setName(tagName + suffix);
                return tagRepository.save(tag);
            })
            .collect(Collectors.toCollection(HashSet::new));
    }
}
