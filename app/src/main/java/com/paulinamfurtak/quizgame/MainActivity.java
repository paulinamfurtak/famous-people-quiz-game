package com.paulinamfurtak.quizgame;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;


import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "QuizGame Activity";
    private List<String> fileNameList;
    private List<String> quizPeopleList;
    private Map<String, Boolean> categoriesMap; // available categories
    private String correctAnswer;
    private int totalGuesses;
    private int correctAnswers;
    private int guessRows; // number of rows displaying choices
    private Random random;
    private Handler handler; // delays loading next picture
    private Animation shakeAnimation; // animation for incorrect guess
    private TextView answerTextView;
    private TextView questionNumberTextView;
    private ImageView personImageView;
    private TableLayout buttonTableLayout; // Table Layout for answer Buttons

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        fileNameList = new ArrayList<String>();
        quizPeopleList = new ArrayList<String>();
        categoriesMap = new HashMap<String, Boolean>();
        guessRows = 1;
        random = new Random();
        handler = new Handler();

        // load the shake animation for incorrect answers
        shakeAnimation = AnimationUtils.loadAnimation(this, R.anim.incorrect_shake);
        shakeAnimation.setRepeatCount(3); // animation repeats 3 times
        // get array of category names from strings.xml
        String[] categoriesNames = getResources().getStringArray(R.array.categoriesList);

        // by default all categories are enabled
        for (String category : categoriesNames)
            categoriesMap.put(category, true);

        // get references to GUI components
        questionNumberTextView = (TextView) findViewById(R.id.questionNumberTextView);
        personImageView = (ImageView) findViewById(R.id.personImageView);
        buttonTableLayout = (TableLayout) findViewById(R.id.buttonTableLayout);
        answerTextView = (TextView) findViewById(R.id.answerTextView);


        questionNumberTextView.setText(
                getResources().getString(R.string.question) + " 1 " +
                        getResources().getString(R.string.of) + " 10");
        // start a new quiz
        resetQuiz();
    }


    private void resetQuiz() {
        // get the image with the AssetManager
        AssetManager assets = getAssets();
        fileNameList.clear();
        try {
            Set<String> categories = categoriesMap.keySet();


            for (String category : categories) {
                if (categoriesMap.get(category)) // if category is available
                {
                    // get a list of all image files in this category
                    String[] paths = assets.list(category);
                    for (String path : paths)
                        fileNameList.add(path.replace(".png", ""));
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error loading image file names", e);
        }

        correctAnswers = 0;
        totalGuesses = 0;
        quizPeopleList.clear(); // empty previous list of figures

        // add 10 random file names to the fileNameList
        int picCounter = 1;
        int numberOfPics = fileNameList.size(); // get number of figures

        while (picCounter <= 10) {
            int randomIndex = random.nextInt(numberOfPics); //

            // get the random file name
            String fileName = fileNameList.get(randomIndex);

            // if the category is available
            if (!quizPeopleList.contains(fileName)) {
                quizPeopleList.add(fileName); // add the file to the list
                ++picCounter;
            }
        }

        loadNextPic(); // start the quiz by loading the first person
    }

    // load the next person after correct answer made
    @SuppressLint("SetTextI18n")
    private void loadNextPic() {
        // remove file name of the next person from quizPeopleList
        String nextImageName = quizPeopleList.remove(0);
        //update the correct answer
        correctAnswer = nextImageName;

        answerTextView.setText(""); //clear answerTextView
        // display the number of the current question
        questionNumberTextView.setText(
                getResources().getString(R.string.question) + " " +
                        (correctAnswers + 1) + " " +
                        getResources().getString(R.string.of) + " 10");
     // extract the category from the next image's name
        String category = nextImageName.substring(0, nextImageName.indexOf('-'));

        // load next image from assets folder
        AssetManager assets = getAssets();
        InputStream stream; //stream used to read in figure
        try {
            // get an InputStream to the asset representing the next person
            stream = assets.open(category + "/" + nextImageName + ".png");
            //
            Drawable person = Drawable.createFromStream(stream, nextImageName);
            personImageView.setImageDrawable(person);
        } catch (IOException e) {
            Log.e(TAG, "Error loading " + nextImageName, e);
        }

        // clear previous answer Buttons from TableRows
        for (int row = 0; row < buttonTableLayout.getChildCount(); ++row)
            ((TableRow) buttonTableLayout.getChildAt(row)).removeAllViews();

        Collections.shuffle(fileNameList); // shuffle file names

        // put the correct answer at the end of fileNameList
        int correct = fileNameList.indexOf(correctAnswer);
        fileNameList.add(fileNameList.remove(correct));

        // get a reference to the LayoutInflater service
        LayoutInflater inflater = (LayoutInflater) getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        // add 3, 6 or 9 answer Buttons based on the value of guessRows
        for (int row = 0; row < guessRows; row++) {
            TableRow currentTableRow = getTableRow(row);

            // put Buttons in currentTableRow
            for (int column = 0; column < 3; column++) {
                // create new Buttons with guess_button.xml
                Button newGuessButton =
                        (Button) inflater.inflate(R.layout.guess_button, null);

                // get person name and set it as newGuessButton's text

                String fileName = fileNameList.get((row * 3) + column);
                newGuessButton.setText(getPersonName(fileName));
                //register answerButtonListener to respond to button clicks
                newGuessButton.setOnClickListener(guessButtonListener);
                currentTableRow.addView(newGuessButton);
            }
        }

        // replace one Button with the correct answer randomly
        int row = random.nextInt(guessRows);
        int column = random.nextInt(3);
        TableRow randomTableRow = getTableRow(row);
        String personName = getPersonName(correctAnswer);
        ((Button) randomTableRow.getChildAt(column)).setText(personName);
    }

    // returns the proper TableRow
    private TableRow getTableRow(int row) {
        return (TableRow) buttonTableLayout.getChildAt(row);
    }

    // returns the person name
    private String getPersonName(String name) {
        return name.substring(name.indexOf('-') + 1).replace('_', ' ');
    }

    // called when an answer is selected
    @SuppressLint("DefaultLocale")
    private void submitGuess(Button guessButton) {
        String guess = guessButton.getText().toString();
        String answer = getPersonName(correctAnswer);
        ++totalGuesses;

        // if the guess is correct
        if (guess.equals(answer)) {
            ++correctAnswers;

            // displays "Correct!" in green
            answerTextView.setText(answer + "!");
            answerTextView.setTextColor(
                    getResources().getColor(R.color.correct_answer));
            disableButtons();

            // if the user has correctly identified 10 people
            if (correctAnswers == 10) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);

                builder.setTitle(R.string.reset_quiz); //title bar string

                // set the AlertDialog's message to display game results
                builder.setMessage(String.format("%d %s, %.02f%% %s",
                        totalGuesses, getResources().getString(R.string.guesses),
                        (1000 / (double) totalGuesses),
                        getResources().getString(R.string.correct)));
                builder.setCancelable(false);
                // add "Reset Quiz" button
                builder.setPositiveButton(R.string.reset_quiz,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                resetQuiz();
                            } // end method onClick
                        } // a lambda expression
                );
//
                AlertDialog resetDialog = builder.create();
                resetDialog.show(); // display the Dialog
            } else // if answer is correct but quiz is not over
            {// load the next person after a 1-second delay
                handler.postDelayed(
                        new Runnable() {
                            @Override
                            public void run() {
                                loadNextPic();
                            }
                        }, 1000); // a lambda expression
            }
        } else //guess is incorrect
        {
            // display the animation
            personImageView.startAnimation(shakeAnimation);
            // display "Incorrect!" in red
            answerTextView.setText(R.string.incorrect_answer);
            answerTextView.setTextColor(
                    getResources().getColor(R.color.incorrect_answer));
            guessButton.setEnabled(false); // disable the incorrect answer
        }
    }

    // method that disabled all answer Buttons
    private void disableButtons() {
        for (int row = 0; row < buttonTableLayout.getChildCount(); ++row) {
            TableRow tableRow = (TableRow) buttonTableLayout.getChildAt(row);
            for (int i = 0; i < tableRow.getChildCount(); ++i)
                tableRow.getChildAt(i).setEnabled(false);
        }
    }

    // create constants for each menu id
    private final int CHOICES_MENU_ID = Menu.FIRST;
    private final int CATEGORIES_MENU_ID = Menu.FIRST + 1;

    // call when the user accesses the options menu

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        // add to the menu options - "Choices" and "Categories"
        menu.add(Menu.NONE, CHOICES_MENU_ID, Menu.NONE, R.string.choices);
        menu.add(Menu.NONE, CATEGORIES_MENU_ID, Menu.NONE, R.string.categories);

        return true; // display the menu
    }

    // call when the user selects an option from the menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // switch the menu id of the user-selected option
        switch (item.getItemId()) {
            case CHOICES_MENU_ID:

                final String[] possibleChoices =
                        getResources().getStringArray(R.array.guessesList);

                AlertDialog.Builder choicesBuilder =
                        new AlertDialog.Builder(this);
                choicesBuilder.setTitle(R.string.choices);
                // add possibleChoices items to the Dialog and set the behaviour when one of them is clicked
                choicesBuilder.setItems(R.array.guessesList,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item) {
                                // update guessRows to match the user's choice
                                guessRows = Integer.parseInt(
                                        possibleChoices[item].toString()) / 3;
                                resetQuiz();
                            } // end method onClick
                        } // a lambda expression
                );

                // create an AlertDialod
                AlertDialog choicesDialog = choicesBuilder.create();
                choicesDialog.show(); // show the Dialog
                return true;
            case CATEGORIES_MENU_ID:
                // get array of categories
                final String[] categoryNames =
                        categoriesMap.keySet().toArray(new String[categoriesMap.size()]);

                // array representing available categories
                boolean[] categoriesEnabled = new boolean[categoriesMap.size()];
                for (int i = 0; i < categoriesEnabled.length; ++i)
                    categoriesEnabled[i] = categoriesMap.get(categoryNames[i]);


                AlertDialog.Builder categoriesBuilder =
                        new AlertDialog.Builder(this);
                categoriesBuilder.setTitle(R.string.categories);

                // replace _ with space in category names for display purposes
                String[] displayNames = new String[categoryNames.length];
                for (int i = 0; i < categoryNames.length; ++i)
                    displayNames[i] = categoryNames[i].replace('_', ' ');

                // add displayNames to the Dialog and set the behaviour when one of the items is clicked
                categoriesBuilder.setMultiChoiceItems(
                        displayNames, categoriesEnabled,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which,
                                                boolean isChecked) {
                                // include or exclude the clicked category depending on whether or not it's checked
                                categoriesMap.put(
                                        categoryNames[which].toString(), isChecked);
                            } // end method onClick
                        } // a lambda expression
                );

                // reset quiz when the "Reset Quiz" Button is pressed
                categoriesBuilder.setPositiveButton(R.string.reset_quiz,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int button) {
                                resetQuiz();
                            } // end method onClick
                        } // a lambda expression
                );


                AlertDialog catogoriesDialog = categoriesBuilder.create();
                catogoriesDialog.show();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // call when a guess Button is touched
    private OnClickListener guessButtonListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            submitGuess((Button) v); // pass selected Button to submitGuess
        } // end method onClick
    };

}