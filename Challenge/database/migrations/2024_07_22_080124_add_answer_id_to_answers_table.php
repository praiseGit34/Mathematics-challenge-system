<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class AddAnswerIdToAnswersTable extends Migration
{
    public function up()
    {
        Schema::table('answers', function (Blueprint $table) {
            $table->string('answerId')->after('id')->nullable();
        });
    }

    public function down()
    {
        Schema::table('answers', function (Blueprint $table) {
            $table->dropColumn('answerId');
        });
    }
}