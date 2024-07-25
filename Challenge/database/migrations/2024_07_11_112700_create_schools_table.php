<?php

use Illuminate\Database\Migrations\Migration;
   use Illuminate\Database\Schema\Blueprint;
   use Illuminate\Support\Facades\Schema;

   class CreateSchoolsTable extends Migration
   {
       public function up()
       {
           Schema::create('schools', function (Blueprint $table) {
               $table->id();
               $table->string('name');
               $table->string('district');
               $table->string('schoolRegNo',50);
               $table->string('emailAddress');
               $table->string('nameOfRep');
               $table->string('password')->nullable()->change();
               $table->timestamps();
           });
       }

       public function down()
       {
           Schema::dropIfExists('schools');
           
       }
   };

