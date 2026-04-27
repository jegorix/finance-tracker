package com.finance.tracker.mapper;

import com.finance.tracker.domain.Account;
import com.finance.tracker.domain.Budget;
import com.finance.tracker.domain.User;
import com.finance.tracker.dto.request.UserRequest;
import com.finance.tracker.dto.response.UserResponse;
import java.util.List;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Component;

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

        response.setAccountIds(Hibernate.isInitialized(user.getAccounts())
                ? user.getAccounts().stream().map(Account::getId).toList()
                : List.of());

        response.setBudgetIds(Hibernate.isInitialized(user.getBudgets())
                ? user.getBudgets().stream().map(Budget::getId).toList()
                : List.of());

        return response;
    }

    public User fromRequest(UserRequest request) {
        if (request == null) {
            return null;
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        return user;
    }

    public UserRequest toRequest(User user) {
        if (user == null) {
            return null;
        }

        UserRequest request = new UserRequest();
        request.setUsername(user.getUsername());
        request.setEmail(user.getEmail());

        return request;
    }
}
