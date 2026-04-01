package com.example.ticketnoob.service;

import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasErrorText;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static org.hamcrest.Matchers.not;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.ticketnoob.R;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class LoginActivityFunctionalTest {

    private void launchActivity() {
        ActivityScenario.launch(LoginActivity.class);
    }

    @Test
    public void login_emptyEmailOrPhone_showsFieldError() {
        launchActivity();

        onView(withId(R.id.etPassword)).perform(replaceText("password123"));
        closeSoftKeyboard();

        onView(withId(R.id.btnLogin)).perform(click());

        onView(withId(R.id.etEmailOrPhone))
                .check(matches(hasErrorText("Provide email or phone")));
    }

    @Test
    public void login_emptyPassword_showsFieldError() {
        launchActivity();

        onView(withId(R.id.etEmailOrPhone)).perform(replaceText("john@test.com"));
        closeSoftKeyboard();

        onView(withId(R.id.btnLogin)).perform(click());

        onView(withId(R.id.etPassword))
                .check(matches(hasErrorText("Provide password")));
    }

    @Test
    public void login_invalidCredentials_buttonEventuallyReturnsToLogin() throws InterruptedException {
        launchActivity();

        onView(withId(R.id.etEmailOrPhone)).perform(replaceText("notarealuser@test.com"));
        onView(withId(R.id.etPassword)).perform(replaceText("wrongpass"));
        closeSoftKeyboard();

        onView(withId(R.id.btnLogin)).perform(click());

        // Immediately after click, button should enter loading state
        onView(withId(R.id.btnLogin))
                .check(matches(withText("Logging in...")))
                .check(matches(not(isEnabled())));

        // Give async Firebase/auth callback time to complete
        Thread.sleep(3000);

        // After callback, button should return to normal state
        onView(withId(R.id.btnLogin))
                .check(matches(withText("Login")))
                .check(matches(isEnabled()));
    }
}