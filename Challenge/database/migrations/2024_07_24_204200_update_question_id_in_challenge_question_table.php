<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class UpdateQuestionIdInChallengeQuestionTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::table('challenge_question', function (Blueprint $table) {
            $table->string('question_id',191)->change();
        });
    }
    
    public function down()
    {
        Schema::table('challenge_question', function (Blueprint $table) {
            $table->integer('question_id',191)->change();
        });
    }
}
