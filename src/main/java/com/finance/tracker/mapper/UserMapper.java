package com.finance.tracker.mapper;

import com.finance.tracker.domain.Account;
import com.finance.tracker.domain.Transaction;
import com.finance.tracker.domain.User;
import com.finance.tracker.dto.request.UserRequest;
import com.finance.tracker.dto.response.UserResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }

        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());

        response.setAccountIds(user.getAccounts() != null ? user.getAccounts().stream().map(Account::getId).toList()
                : null);

        response.setTransactionIds(
                user.getTransactions() != null ? user.getTransactions().stream().map(Transaction::getId).toList()
                        : null);

        return response;
    }

    public User fromRequest(UserRequest request,
            List<Account> accounts,
            List<Transaction> transactions) {

        if (request == null) {
            return null;
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        if (accounts != null) {
            user.setAccounts(accounts);
            accounts.forEach(a -> a.setUser(user));
        }
        if (transactions != null) {
            user.setTransactions(transactions);
            transactions.forEach(t -> t.setUser(user));
        }
        return user;
    }

    public UserRequest toRequest(User user) {
        if (user == null) {
            return null;
        }

        UserRequest request = new UserRequest();
        request.setUsername(user.getUsername());
        request.setEmail(user.getEmail());

        request.setAccountIds(user.getAccounts() != null
                ? user.getAccounts().stream().map(Account::getId).collect(Collectors.toList())
                : null);

        request.setTransactionIds(user.getTransactions() != null
                ? user.getTransactions().stream().map(Transaction::getId).collect(Collectors.toList())
                : null);

        return request;
    }
}
