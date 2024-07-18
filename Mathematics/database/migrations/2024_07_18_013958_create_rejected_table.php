<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateRejectedTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::create('rejected', function (Blueprint $table) {
            $table->id();
            $table->unsignedBigInteger('schoolRegNo');
            $table->string('emailAddress');
            $table->unsignedBigInteger('applicantId');
            $table->string('userName');
            $table->string('imagePath');
            $table->string('firstName');
            $table->string('lastName');
            $table->string('password');
            $table->date('dateOfBirth');
            $table->timestamps();
        
            $table->foreign('schoolRegNo')->references('id')->on('school');
            $table->foreign('applicantId')->references('id')->on('applicant');
        });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::dropIfExists('rejected');
    }
}
