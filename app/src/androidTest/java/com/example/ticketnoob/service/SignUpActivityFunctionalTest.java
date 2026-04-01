package com.example.ticketnoob.service;

import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasErrorText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import com.example.ticketnoob.ui.activities.SignUpActivity;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.ticketnoob.R;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SignUpActivityFunctionalTest {

    private void launchActivity() {
        ActivityScenario.launch(SignUpActivity.class);
    }

    @Test
    public void register_missingEmailAndPhone_showsErrorOnBothFields() {
        launchActivity();

        onView(withId(R.id.etName)).perform(replaceText("John Doe"));
        onView(withId(R.id.etPassword)).perform(replaceText("password123"));
        closeSoftKeyboard();

        onView(withId(R.id.btnRegister)).perform(click());

        onView(withId(R.id.etEmail))
                .check(matches(hasErrorText("Provide email OR phone")));

        onView(withId(R.id.etPhone))
                .check(matches(hasErrorText("Provide email OR phone")));
    }

    @Test
    public void register_invalidEmail_showsEmailError() {
        launchActivity();

        onView(withId(R.id.etName)).perform(replaceText("John Doe"));
        onView(withId(R.id.etEmail)).perform(replaceText("invalid-email"));
        onView(withId(R.id.etPassword)).perform(replaceText("password123"));
        closeSoftKeyboard();

        onView(withId(R.id.btnRegister)).perform(click());

        onView(withId(R.id.etEmail))
                .check(matches(hasErrorText("Invalid email format")));
    }

    @Test
    public void register_invalidPhone_showsPhoneError() {
        launchActivity();

        onView(withId(R.id.etName)).perform(replaceText("John Doe"));
        onView(withId(R.id.etPhone)).perform(replaceText("123abc"));
        onView(withId(R.id.etPassword)).perform(replaceText("password123"));
        closeSoftKeyboard();

        onView(withId(R.id.btnRegister)).perform(click());

        onView(withId(R.id.etPhone))
                .check(matches(hasErrorText("Invalid phone format")));
    }

    @Test
    public void register_missingName_showsNameError() {
        launchActivity();

        onView(withId(R.id.etEmail)).perform(replaceText("john@test.com"));
        onView(withId(R.id.etPassword)).perform(replaceText("password123"));
        closeSoftKeyboard();

        onView(withId(R.id.btnRegister)).perform(click());

        onView(withId(R.id.etName))
                .check(matches(hasErrorText("Name required")));
    }

    @Test
    public void register_missingPassword_showsPasswordError() {
        launchActivity();

        onView(withId(R.id.etName)).perform(replaceText("John Doe"));
        onView(withId(R.id.etEmail)).perform(replaceText("john@test.com"));
        closeSoftKeyboard();

        onView(withId(R.id.btnRegister)).perform(click());

        onView(withId(R.id.etPassword))
                .check(matches(hasErrorText("Password required")));
    }

    @Test
    public void register_validEmail_submitsSuccessfully() throws InterruptedException {
        launchActivity();

        String uniqueEmail = "john" + System.currentTimeMillis() + "@test.com";

        onView(withId(R.id.etName)).perform(replaceText("John Doe"));
        onView(withId(R.id.etEmail)).perform(replaceText(uniqueEmail));
        onView(withId(R.id.etPassword)).perform(replaceText("password123"));
        closeSoftKeyboard();

        onView(withId(R.id.btnRegister)).perform(click());

        Thread.sleep(3000);
    }

    @Test
    public void register_validPhone_submitsSuccessfully() throws InterruptedException {
        launchActivity();

        String uniquePhone = "514" + (System.currentTimeMillis() % 10000000L);

        onView(withId(R.id.etName)).perform(replaceText("John Doe"));
        onView(withId(R.id.etPhone)).perform(replaceText(uniquePhone));
        onView(withId(R.id.etPassword)).perform(replaceText("password123"));
        closeSoftKeyboard();

        onView(withId(R.id.btnRegister)).perform(click());

        Thread.sleep(3000);
    }
}